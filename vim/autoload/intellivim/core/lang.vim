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
    " Arguments:
    "  - "base" The text which matches should match (see :h completion-functions)
    "  - "completion" Completion dict
    " Returns: 1 if the completion should be included, else 0

    return stridx(a:completion.body, a:base) == 0
endfunction " }}}

function! intellivim#core#lang#CodeComplete(findstart, base) " {{{
    if !intellivim#InProject()
        return a:findstart ? -1 : []
    endif

    let filetype = &ft
    if a:findstart

        " make sure the file on disk is up to date
        call intellivim#SilentUpdate()

        let findStartName = "intellivim#" . filetype . "#lang#FindStart"
        if !exists("*" . findStartName)
            let findStartName = "intellivim#core#lang#FindStart"
        endif
        let FindStart = function(findStartName)
        return FindStart()
    else
        let command = intellivim#NewCommand("complete")
        let command.offset = intellivim#GetOffset()
        let result = intellivim#client#Execute(command)
        if intellivim#ShowErrorResult(result)
            return
        endif

        if has_key(result.result, 'problems') && !len(result.result.problems)
            " we fixed a problem!

            " reload the file (thank you, eclim)
            let func = "intellivim#core#ReloadFile({'newOffset': " 
                        \ . result.newOffset . "}, {'update': 0})"
            call feedkeys("\<c-e>\<c-r>=" . func . "\<cr>", 'n')

            " restart completion
            silent call s:RestartCompletion()
            return -1
            " TODO if not-empty, we could proactively show the errors....
        endif

        let clean = "intellivim#" . filetype . "#lang#CleanCompletion"
        if !exists("*" . clean)
            let clean = "intellivim#core#lang#CleanCompletion"
        endif
        let CleanCompletion = function(clean)

        let completions = []
        for item in result.result.completions

            " clean it up
            if CleanCompletion(a:base, item)
                call add(completions, {
                    \ 'word': item.body,
                    \ 'menu': item.detail,
                    \ 'info': item.doc,
                    \ 'dup': 1
                    \ })
            endif
        endfor

        return completions
    endif

endfunction " }}}

"
" Private utils
"
function! s:RestartCompletion() " {{{
    " TODO For YCM, it'd be nicer if we could just replay
    "  the last keypress, so the first suggestion isn't
    "  necessarily selected....
    call feedkeys("\<c-x>\<c-o>", 'n')
endfunction " }}}

" vim:ft=vim:fdm=marker
