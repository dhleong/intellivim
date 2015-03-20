" Author: Daniel Leong
"
" A lot of the UI design is inspired by eclim

" Global vars {{{
let s:default_locate_type = 'FILE'
let s:default_update_time = 2000
" }}}

function! intellivim#core#locate#Locate(type) " {{{
    " Open a window for locating files in the project
    " Arguments:
    "  - type One of "file" or "class"

    let type = toupper(a:type)
    if a:type == ''
        let type = s:default_locate_type
    endif

    let command = intellivim#NewCommand("locate")
    let command.type = type

    " TODO configurable 'method'
    let config = {
        \ 'command': command,
        \ 'method': "split",
        \ 'onSelect': function("s:OpenFile")
        \ }

    call intellivim#core#locate#OpenSearchWindow(config)

endfunction " }}}

function! intellivim#core#locate#OpenSearchWindow(config) " {{{
    " Config: A dictionary:
    "  - command: Command object to send. The search pattern
    "             will be filled into the "pattern" key
    "  - onSelect: A Function(config, item) that will be called
    "              when an item is selected. You get this config
    "              object again so you can store extra config in it

    " TODO ensure the server is running first

    " save some info
    let bufno = bufnr('%')
    let winno = winnr()
    let winrestcmd = winrestcmd()
    let last_updatetime = &updatetime

    " prepare the results window
    topleft 12split [Locate\ Results]
    set filetype=locate-results
    setlocal nonumber nowrap noswapfile nobuflisted
    setlocal buftype=nofile bufhidden=delete
    let results_bufno = bufnr('%')

    " prepare the input window
    " TODO show project name?
    exec 'topleft 1split [Locate in project]'
    setlocal modifiable
    call setline(1, '> ')
    call cursor(1, col('$'))
    set filetype=locate-prompt
    syntax match Keyword /^>/
    setlocal winfixheight
    setlocal nonumber nolist noswapfile nobuflisted
    setlocal buftype=nofile bufhidden=delete

    " save some more stuff
    let b:bufno = bufno
    let b:winno = winno
    let b:results_bufno = results_bufno
    let b:selected = 1
    let b:winrestcmd = winrestcmd
    let b:config = a:config
    let b:completions = []

    set updatetime=300

    augroup locate_file_init
        autocmd!
        autocmd BufEnter <buffer> nested startinsert! | let &updatetime = 300
        autocmd BufLeave \[Locate*\] call <SID>CloseLocateWindow()
        exec 'autocmd InsertLeave <buffer> ' .
                    \ 'let &updatetime = ' . last_updatetime . ' | ' .
                    \ 'doautocmd BufWinLeave | bw | ' .
                    \ 'doautocmd BufWinLeave | bw ' . b:results_bufno . ' | ' .
                    \ 'exe bufwinnr(' .  b:bufno . ') | ' .
                    \ 'doautocmd BufEnter | ' .
                    \ 'doautocmd WinEnter | ' .
                    \ winrestcmd
        exec 'autocmd WinEnter <buffer=' . b:results_bufno .'> '
                    \ 'exec bufwinnr(' . bufnr('%') . ') "winc w"'
    augroup END

    " bindings to accept, move, etc.
    inoremap <buffer> <silent> <up> <c-r>=<SID>OnNavigate('up')<cr>
    inoremap <buffer> <silent> <c-k> <c-r>=<SID>OnNavigate('up')<cr>
    inoremap <buffer> <silent> <down> <c-r>=<SID>OnNavigate('down')<cr>
    inoremap <buffer> <silent> <c-j> <c-r>=<SID>OnNavigate('down')<cr>
    inoremap <buffer> <silent> <cr> <c-r>=<SID>OnSelectFile()<cr>

    " wait for input!
    startinsert!
    call s:RestartCompletionOnInput()

endfunction " }}}

" 
" onSelect actions {{{
"

function! s:OpenFile(config, item) " {{{
    let item = a:item
    let config = a:config

    let path = item.path
    exe config.method . ' ' . escape(path, ' ')
endfunction " }}}

" }}}

"
" Util and callbacks
"

function! s:CloseLocateWindow() " {{{
    if bufname('%') !~ '^\[Locate.*\]$'
        " shouldn't happen
        let winno = bufwinnr('\[Locate in *\]')
        exe winno . 'winc w'
    endif

    " just trigger the InsertLeave handling
    stopinsert
    doautocmd InsertLeave
    autocmd! locate_file_init
endfunction " }}}

function! s:OnNavigate(dir) " {{{
    " pause completion while navigating
    augroup locate_file
        autocmd!
    augroup END

    " convert to index briefly for sanity
    let index = b:selected - 1
    if a:dir == 'up'
        let index = index - 1
        if index < 0
            let index = len(b:completions) - 1
        endif
    elseif a:dir == 'down'
        let index = index + 1
        if index >= len(b:completions)
            let index = 0
        endif
    else
        " just reset
        let index = 0
    endif

    " save
    let selected = index + 1
    let b:selected = selected

    " update highlighting
    let winno = winnr()
    noautocmd exe bufwinnr(b:results_bufno) . 'winc w'
    syntax clear
    exec 'syntax match PmenuSel /\%' . selected . 'l.*/'
    exec 'call cursor(' . selected . ', 1)'
    let save_scrolloff = &scrolloff
    let &scrolloff = 5
    normal! zt
    let &scrolloff = save_scrolloff

    " pop back and prepare search again
    noautocmd exe winno . 'winc w'
    call s:RestartCompletionOnInput()

    " don't mess up my input!
    return ''
endfunction " }}}

function! s:OnSelectFile() " {{{
    let selectedIndex = b:selected - 1
    if selectedIndex < 0 || selectedIndex >= len(b:completions)
        return
    endif

    let item = b:completions[selectedIndex]
    let config = b:config

    " close the window and act
    call s:CloseLocateWindow()
    call config.onSelect(config, item)
endfunction " }}}

function! s:RestartCompletion() " {{{
    augroup locate_file
        autocmd!
        autocmd CursorHoldI <buffer> call <SID>TriggerCompletion()
    augroup END
endfunction " }}}

function! s:RestartCompletionOnInput() " {{{
    augroup locate_file
        autocmd!
        autocmd CursorMovedI <buffer> call <SID>RestartCompletion()
    augroup END
endfunction " }}}

function! s:TriggerCompletion() " {{{

    let line = getline('.')
    if line !~ '^> '
        call setline(1, substitute(line, '^>\?\s*', '> \1', ''))
        call cursor(1, 3)
        let line = getline('.')
    endif

    let display = []
    let pattern = substitute(line, '^>\s*', '', '')
    if pattern !~ '^\s*$'
        " something to search with
        let command = b:config.command
        let command.pattern = pattern
        let result = intellivim#client#Execute(command)
        if intellivim#ShowErrorResult(result)
            return
        endif

        let b:completions = result.result
        let display = map(copy(result.result), 'v:val.display')
    endif

    " update the results
    let winnr = winnr()
    noautocmd exe bufwinnr(b:results_bufno) . 'winc w'
    setlocal modifiable
    1,$delete _
    call append(1, display)
    1,1delete _
    setlocal nomodifiable
    exe winnr . 'winc w'

    call s:OnNavigate('reset')

endfunction " }}}

" vim:ft=vim:fdm=marker
