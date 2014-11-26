" Author: Daniel Leong
"

function! intellivim#java#Setup() " {{{

    " define commands {{{
    if !exists(":JavaOptimizeImports")
        command -nargs=0 JavaOptimizeImports
            \ call intellivim#java#OptimizeImports()
    endif
    " }}}

endfunction " }}}

function! intellivim#java#OptimizeImports() " {{{

    let command = intellivim#NewCommand("java_import_optimize")
    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    call intellivim#core#ReloadFile()

endfunction " }}}

" vim:ft=vim:fdm=marker
