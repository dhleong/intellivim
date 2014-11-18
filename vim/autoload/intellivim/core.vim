
function! intellivim#core#Setup() " {{{

    augroup intellivim_core
        autocmd!
        autocmd BufWritePost <buffer> call intellivim#core#Update()
    augroup END

endfunction " }}}

function! intellivim#core#Update() " {{{
    let command = intellivim#NewCommand("get_problems")
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    " mark problems
    let list = []
    for problem in result.result
        call add(list, s:ProblemToLocationEntry(problem))
    endfor

    call setloclist(0, list, 'r')
    call intellivim#signs#Update()

endfunction " }}}

function s:ProblemToLocationEntry(problem) " {{{

    let prob = a:problem

    if !has_key(prob, 'file')
        " if it doesn't have a file already,
        "  it's just for the current file
        let prob.file = expand('%:p')
    endif

    return {
        \ 'filename': prob.file,
        \ 'lnum': prob.line,
        \ 'col': prob.col,
        \ 'text': prob.description,
        \ 'type': strpart(prob.severity, 0, 1),
        \ 'nr': prob.id
        \ }
endfunction " }}}

" vim:ft=vim:fdm=marker
