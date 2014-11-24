" Author: Daniel Leong
"

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
        nmap <buffer> q :q<cr>
    endif

    let b:last_bufno = bufno

endfunction " }}}
