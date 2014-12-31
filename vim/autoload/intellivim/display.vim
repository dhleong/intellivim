" Author: Daniel Leong
"

function! s:ExecuteAndFillWindow(command) " {{{
    " Execute a command and append the results
    "  line-by-line to the current window

    let result = intellivim#client#Execute(a:command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    setlocal modifiable

    " prepare contents
    let contents = split(result.result, '\n')
    call append(0, contents)
    retab
    norm! gg

    setlocal wrap
    setlocal nomodifiable
    setlocal nolist
    setlocal noswapfile
    setlocal nobuflisted
    setlocal buftype=nofile
    setlocal bufhidden=wipe

    nnoremap <buffer> <silent> q :q<cr>
endfunction " }}}

function! intellivim#display#PreviewWindowFromCommand(name, command) " {{{
    " Show a preview window whose contents are the results
    "  of executing the given command

    exe 'pedit +:call\ s:ExecuteAndFillWindow(a:command) ' . a:name

endfunction " }}}

function! intellivim#display#TempWindow(name, contents, ...) " {{{
    " Prepare a temporary window with the given name and contents.
    " Optionally, a dict may be provided to specify extra options:
    " Options:
    "  readonly (default: 1)
    "  split (default: belowright): command to use to create the window
    "  orientation (default: horizontal): horizontal/vertical
    "  height (default: 10): height if horizontal
    "  width (default: 50): width if vertical

    let options = a:0 > 0 ? a:1 : {}
    let bufno = bufnr('%')

    let name = escape(a:name, ' ')
    if has('unix')
        let name = escape(name, '[]')
    endif

    let existingWinNr = bufwinnr(name)
    if existingWinNr == -1
        let orient = get(options, 'orientation', 'horizontal')
        if orient == 'vertical'
            let location = get(options, 'location', 'belowright')
            let width = get(options, 'width', 50)
            let split_cmd = location . " vertical " . width . " sview "
            silent! noautocmd exec "keepalt " . split_cmd . name
        else
            let location = get(options, 'location', 'botright')
            let height = get(options, 'height', 10)
            let split_cmd = location . " " . height . " sview "
            silent! noautocmd exec "keepalt " . split_cmd . name
        endif

        setlocal nowrap
        setlocal winfixheight
        setlocal noswapfile
        setlocal nobuflisted
        setlocal buftype=nofile
        setlocal bufhidden=wipe
        silent doautocmd WinEnter
    else
        " reuse the existing buffer
        if existingWinNr != winnr()
            exe existingWinNr . 'winc w'
            silent doautocmd WinEnter
        endif
    endif

    " clear anything existing and insert our content
    setlocal modifiable
    setlocal noreadonly
    silent 1,$delete _
    call append(0, a:contents)
    retab

    " pop to the top
    norm! gg

    if get(options, 'readonly', 1)
        setlocal nomodified
        setlocal nomodifiable
        setlocal readonly
        nmap <buffer> <silent> q :q<cr>
    endif

    let b:last_bufno = bufno

endfunction " }}}

function! intellivim#display#ScrollBuffer(bufNo, ...) " {{{
    " Scroll some other buffer to the end, or to a specific line
    " This is a nop if python is not available
    " Options:
    "  target (default: "end"): Where to scroll to. May be "start",
    "    "end", or a line number
    "  ifAt (default: -1): If >= 0, a line number that the cursor
    "    must currently be on for this operation to proceed

    if !has('python')
        return
    endif

    let target = a:0 > 0 ? a:1 : "end"
    let ifAt = a:0 > 1 ? a:2 : -1
    let bufno = a:bufNo

py << PYEOF
import vim
bufno = int(vim.eval('bufno'))
buf = vim.buffers[bufno]
if buf:
    target = vim.eval('target')
    ifAt = vim.eval('ifAt')

    line = 0
    if 'end' == target:
        line = len(buf)
    elif 'start' == target:
        line = 0
    else:
        line = int(target)

    # find windows for this buffer
    scrollWin = None
    for tab in vim.tabpages:
        for win in tab.windows:
            if win.buffer.number == buf.number:
                # scroll to bottom if still there
                row, col = win.cursor
                if ifAt >= 0 or row == ifAt:
                    win.cursor = [line, col]
                    scrollWin = win

    vim.command("redraw!")

    if scrollWin:
        # sometimes needed
        vim.command("%dwinc w" % scrollWin.number)
        vim.command("winc p")

PYEOF

endfunction " }}}

" vim:ft=vim:fdm=marker
