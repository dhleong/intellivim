" Author: Daniel Leong
"

function! intellivim#java#OptimizeImports() " {{{

    let command = intellivim#NewCommand("java_import_optimize")
    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    call intellivim#core#ReloadFile()

endfunction " }}}

" vim:ft=vim:fdm=marker
