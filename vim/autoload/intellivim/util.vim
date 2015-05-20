" Author: Daniel Leong
"

function! intellivim#util#Echo(message) " {{{
    " TODO
    echo a:message
endfunction " }}}

function! intellivim#util#EchoError(message) " {{{
    " TODO
    echo a:message
endfunction " }}}

function! intellivim#util#Pad(string, length, ...) " {{{
    " Pad the given string to the given length,
    "  optionally supplying the padding char
    let padding = a:0 ? a:1 : ' '

    let actualLen = len(a:string)
    if actualLen > a:length
        return strpart(a:string, 0, a:length)
    endif

    let pad = ''
    for i in range(1, a:length - actualLen)
        let pad = pad . padding
    endfor

    return a:string . pad

endfunction " }}}

function! intellivim#util#Strip(string) " {{{
    " Strip whitespace from the ends of the string
    return substitute(a:string, '^\s*\(.\{-}\)\s*$', '\1', '')
endfunction " }}}

" vim:ft=vim:fdm=marker
