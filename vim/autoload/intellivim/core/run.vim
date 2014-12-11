" Author: Daniel Leong
"

let s:IVTerminateLaunchOnBufferClosed = 1

" Python functions {{{
" Requiring python is gross, but it's the only way to append to
"  a buffer that isn't visible, and that is surely required
function! s:append(bufno, type, line) " {{{
    if !has('python')
        return
    endif

    " prepare vars so python can pick them up
    let bufnr = a:bufno
    let ltype = a:type
    let lines = split(a:line, '\r', 1)

py << PYEOF
import vim
bufnr = int(vim.eval('bufnr')) # NB int() is crucial
buf = vim.buffers[bufnr]
if buf:
    lines = vim.eval('lines')
    ltype = vim.eval('ltype')
    prefixed = map(lambda l: "%s>%s" % (ltype, l), lines)
    oldEnd = len(buf)

    buf.options['readonly'] = False
    buf.options['modifiable'] = True
    buf.append(prefixed)
    buf.options['readonly'] = True
    buf.options['modifiable'] = False

    # find windows for this buffer
    scrollWin = None
    for tab in vim.tabpages:
        for win in tab.windows:
            if win.buffer.number == buf.number:
                # scroll to bottom if still there
                row, col = win.cursor
                if row == oldEnd:
                    win.cursor = [len(buf), col]
                    scrollWin = win

PYEOF

    redraw!

endfunction " }}}
" }}}


"
" Public functions
"

function! intellivim#core#run#CompleteRunConfigs(argLead, cmdLine, cursorPos) " {{{
    let configs = s:GetRunConfigs()
    let configs = map(configs, 'v:val.name')
    return filter(configs, 'v:val =~ "^' . a:argLead . '"')
endfunction " }}}

function! intellivim#core#run#Run(configuration) " {{{
    let command = intellivim#NewCommand("run")
    if !empty(a:configuration)
        let command.configuration = a:configuration
    endif

    let result = intellivim#client#Execute(command)
    call intellivim#ShowErrorResult(result)
endfunction " }}}

function! intellivim#core#run#RunList() " {{{
    let configs = s:GetRunConfigs()
    if len(configs) == 0
        call intellivim#util#Echo("No launch configs found")
        return
    endif
    
    let pad = 0
    for config in configs
        let pad = len(config.name) > pad ? len(config.name) : pad
    endfor

    let output = []
    for config in configs
        call add(output,
            \ intellivim#util#Pad(config.name, pad) . ' - ' . config.type)
    endfor
    call eclim#util#Echo(join(output, "\n"))

endfunction " }}}

function! intellivim#core#run#TerminateAllLaunches() " {{{
    let command = intellivim#NewCommand("terminate")
    let result = intellivim#client#Execute(command)
    call intellivim#ShowErrorResult(result)
endfunction " }}}

function! intellivim#core#run#TerminateLaunch(launchId) " {{{
    let command = intellivim#NewCommand("terminate")
    let command.id = a:launchId
    let result = intellivim#client#Execute(command)
    call intellivim#ShowErrorResult(result)
endfunction " }}}


"
" Callbacks
"

function! intellivim#core#run#onPrepareOutput(launchId) " {{{
    let current = winnr()

    " is there a terminated launch window?
    " NB missing open bracket is intentional, and
    "  it returns the wrong buffer if not omitted
    let terminated = 'TERMINATED ' . a:launchId . ']'
    let terminatedBuf = bufnr(terminated)
    if terminatedBuf != -1
        exe 'bdelete ' . terminatedBuf
    endif

    call intellivim#display#TempWindow('[' . a:launchId . ' Output]', [])
    let no = bufnr('%')
    let b:launch_id = a:launchId

    augroup intellivim_run_cleanup
        autocmd!
        autocmd VimLeavePre * call intellivim#core#run#TerminateAllLaunches()
    augroup END

    " TODO setting
    if s:IVTerminateLaunchOnBufferClosed
        exe 'autocmd BufWipeout <buffer> call intellivim#core#run#TerminateLaunch("' .
                    \ b:launch_id . '")'
    else
        " need to keep the buffer around, then
        setlocal bufhidden=hide
    endif

    " supply a Terminate command
    exe 'command -nargs=0 -buffer Terminate ' .
                \ ':call intellivim#core#run#TerminateLaunch("' .
                \ b:launch_id . '")'

    " beautiful highlighting for error lines vs out> lines
    syntax region Error matchgroup=Quote start=/stderr>/ end=/\n/ concealends oneline
    syntax region Normal matchgroup=Quote start=/stdout>/ end=/\n/ concealends oneline
    syntax region MoreMsg matchgroup=Quote start=/system>/ end=/\n/ concealends oneline

    set conceallevel=3
    set concealcursor=nc

    " pop back and show
    exe current . "winc w"
    redraw!
    return no
endfunction! " }}}

function! intellivim#core#run#onOutput(bufNo, type, line) " {{{
    if has('python')
        call s:append(a:bufNo, a:type, a:line)
    endif
endfunction " }}}

function! intellivim#core#run#onCancelled(bufNo) " {{{
    call s:append(a:bufno, "system", "Launch Cancelled")
    call intellivim#core#run#onTerminated(a:bufNo)
endfunction " }}}

function! intellivim#core#run#onTerminated(bufNo) " {{{
    if !has('python')
        return
    endif

    " rename the buffer
    let bufnr = a:bufNo

py << PYEOF
import vim
bufnr = int(vim.eval('bufnr'))
buf = vim.buffers[bufnr]
if buf is not None:
    buf.name = "[TERMINATED %s]" % buf.vars['launch_id']
PYEOF

    redraw!
endfunction " }}}

"
" Private functions
"

function! s:GetRunConfigs() " {{{

    if !intellivim#InProject()
        call intellivim#ShowErrorResult("No project found")
        return []
    endif

    let command = intellivim#NewCommand("run_list")
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return []
    endif

    return result.result
endfunction " }}}

" vim:ft=vim:fdm=marker
