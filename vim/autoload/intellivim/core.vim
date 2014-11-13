
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

    " TODO mark problems
    let b:result = result
endfunction " }}}

" vim:ft=vim:fdm=marker
