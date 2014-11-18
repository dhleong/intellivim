" The functions in this file draw heavy inspiration
"  from previous work on eclim (c) Eric Van Dewoestine
"  without which I would have had no idea where to 
"  begin. Many thanks!

let s:sign_types = ['w', 'e']

let s:sign_markers = {
    \ 'w': '> ',
    \ 'e': '>>'
    \ }

let s:sign_highlights = {
    \ 'w': 'WarningMsg',
    \ 'e': 'Error'
    \ }

function! intellivim#signs#Define(name, text, highlight) " {{{
    exe "sign define " . a:name 
        \ . " text=" . a:text
        \ . " texthl=" . a:highlight
endfunction " }}}

function! intellivim#signs#PrepareSigns(items) " {{{
    " Ensure signs are defined for the types in items
    " (we'll just define them all)

    for type in s:sign_types
        call intellivim#signs#Define(type,
            \ s:sign_markers[type], s:sign_highlights[type])
    endfor

endfunction " }}}

function! intellivim#signs#Place(type, bufno, line) " {{{
    " Place a sign at the given line in the given buffer
    let id = a:line
    exe "sign place " . id . 
        \ " line=" . a:line .
        \ " name=" . a:type
        \ " buffer=" . a:bufno
endfunction " }}}

function! intellivim#signs#UnplaceAll(bufno) " {{{
    " Unplace all signs in the given buffer
    exe "sign unplace * " .
        \ " buffer=" . a:bufno
endfunction " }}}


function! intellivim#signs#Update() " {{{
    " Update signs in current buffer to match the location list
    " and quickfix list

    if !has('signs') || &ft == 'qf'
        return
    endif

    let save_lazy = &lazyredraw
    set lazyredraw

    " TODO remove existing signs
    let qflist = filter(getqflist(),
        \ 'bufnr("%") == v:val.bufnr')
    let loclist = getloclist(0)
    let items = qflist + loclist

    " Place signs
    let bufno = bufnr('%')
    call intellivim#signs#PrepareSigns(items)
    for item in items
        let line = item.lnum
        let type = tolower(item.type)
        call intellivim#signs#Place(type, bufno, line)
    endfor

  let &lazyredraw = save_lazy
endfunction " }}}

" vim:ft=vim:fdm=marker
