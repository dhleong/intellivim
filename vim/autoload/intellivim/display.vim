" Author: Daniel Leong
"

function! intellivim#display#PreviewWindowFromCommand(name, command) " {{{
    " Show a preview window whose contents are the results
    "  of executing the given command

    exe 'pedit +:call\ s:ExecuteAndFillWindow(a:command) ' . a:name

endfunction " }}}

function! intellivim#display#PromptList(config) " {{{
    " Prompt the user to pick from a list of choices.
    " The argument is a dict defined as follows:
    " Arguments:
    " - title Title of the window
    " - list List of strings to choose from
    " - onSelect Function called when a choice was selected.
    "            Will be called as `onSelect([selectArgs...], choice)`
    "            where `selectArgs` is optionally those passed in this
    "            dict, and `choice` is the string chosen by the user
    " - onCancel (optional) Function called when the user cancels
    " - selectArgs (optional) Arguments to be passed to onSelect
    " - cancelArgs (optional) Arguments to be passed to onCancel
    " - autoDismiss (optional; default: true) If true, the prompt
    "            window will be dismissed after `onSelect` is called

    let config = a:config
    let title = config.title
    let list = config.list

    let contents = []
    let index = 0
    for choice in list
        call add(contents, index . ": " . choice)
        let index = index + 1
    endfor

    call intellivim#display#TempWindow("[" . title . "]", contents)
    let b:prompt_config = config
    nnoremap <buffer> <cr> :call <SID>ListPromptSelect()<cr>
    nnoremap <buffer> q :call <SID>ListPromptCancel()<cr>
    nnoremap <buffer> <c-c> :call <SID>ListPromptCancel()<cr>
    " TODO highlight currently selected item
    " TODO support multi-line items nicely
    " TODO support numbered shortcuts

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

    " clear the (always empty) last line and pop to the top
    silent $delete _
    call cursor(1, 1)

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

"
" Calbacks
"

function s:ListPromptCancel() " {{{
    let config = b:prompt_config

    norm! ZZ

    if has_key(config, 'onCancel')
        if type(get(config, 'cancelArgs')) == type([])
            call call(config.onCancel, config.cancelArgs)
        else
            call config.onCancel()
        endif
    endif

endfunction " }}}

function s:ListPromptSelect() " {{{
    let config = b:prompt_config

    let line = getline('.')
    let parts = split(line, ':')
    if len(parts) == 1
        " nothing to be done
        call intellivim#util#EchoError("Not a valid choice")
    endif

    let index = str2nr(parts[0])
    let choice = config.list[index]

    " close now, because onSelect might pop
    "  back to a specific place
    if 0 != get(config, 'autoDismiss', 1)
        norm! ZZ
    endif

    if type(get(config, 'selectArgs')) == type([])
        call call(config.onSelect, config.selectArgs + [choice])
    else
        call config.onSelect(choice)
    endif

endfunction " }}}

"
" Private utils
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

" vim:ft=vim:fdm=marker
