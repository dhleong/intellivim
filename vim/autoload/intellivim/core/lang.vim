" Author: Daniel Leong
"

function! intellivim#core#lang#FindStart() " {{{
    " default FindStart implementation

    let line = getline('.')
    let start = col('.') - 1

    "exceptions that break the rule
    if line[start] =~ '\.'
      let start -= 1
    endif

    while start > 0 && line[start - 1] =~ '\w'
      let start -= 1
    endwhile

    return start
endfunction " }}}

function! intellivim#core#lang#CleanCompletion(base, completion) " {{{
    " default CleanCompletion implementation

endfunction " }}}

function! intellivim#core#lang#CodeComplete(findstart, base)
    if !intellivim#InProject()
        return a:findstart ? -1 : []
    endif

    let filetype = &ft
    if a:findstart

        " make sure the file on disk is up to date
        silent noautocmd update

        let findStart = "intellivim#" . filetype . "#lang#FindStart"
        if !exists("*" . findStart)
            let findStart = "intellivim#core#lang#FindStart"
        endif

        exe "let start = " . findStart . "()"
        return start
    else
        let command = intellivim#NewCommand("complete")
        let command.offset = intellivim#GetOffset()
        let result = intellivim#client#Execute(command)
        if intellivim#ShowErrorResult(result)
            return
        endif

        let clean = "intellivim#" . filetype . "#lang#CleanCompletion"
        if !exists("*" . clean)
            let clean = "intellivim#core#lang#CleanCompletion"
        endif

        let completions = []
        for item in result.result

            " clean it up
            exe "call " . clean . "(a:base, item)"

            call add(completions, {
                \ 'word': item.body,
                \ 'menu': item.detail,
                \ 'info': item.doc,
                \ 'dup': 1
                \ })
        endfor

        return completions
    endif

endfunction

" vim:ft=vim:fdm=marker
