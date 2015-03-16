" Author: Daniel Leong
"

" Variables {{{
" never run core Setup in these filetypes
let s:alwaysIgnoredFiletypes = ["gitcommit"]
" }}}

function! intellivim#core#Setup() " {{{

    " always setup :Locate
    if 2 != exists(":Locate") " compat with other Locate*
        command -nargs=? Locate
            \ :call intellivim#core#locate#Locate('<args>')
    endif

    if &previewwindow || &ft == '' || s:ShouldIgnoreFiletype(&ft)
        " preview window, or no real ft; don't do anything else
        return
    endif

    augroup intellivim_core
        autocmd!
        autocmd BufWritePost <buffer> call intellivim#core#Update()
    augroup END

    " also, update now
    call intellivim#core#Update()

    " prepare omnifunc
    setlocal omnifunc=intellivim#core#lang#CodeComplete

    " define commands {{{
    if !exists(":FixProblem")
        command -nargs=0 FixProblem
            \ call intellivim#core#problems#FixProblem()
    endif

    if !exists(":GetDocumentation")
        command -nargs=0 GetDocumentation
            \ call intellivim#core#GetDocumentation()
    endif

    if !exists(":GotoDeclaration")
        command -nargs=0 GotoDeclaration
            \ call intellivim#core#GotoDeclaration()
    endif

    if !exists(":Implement")
        command -nargs=0 Implement
            \ call intellivim#core#impl#ShowImplementables()
    endif

    if !exists(":RunProject")
        command -nargs=?
            \ -complete=customlist,intellivim#core#run#CompleteRunConfigs
            \ RunProject :call intellivim#core#run#Run('<args>')
        command -nargs=0
            \ RunList :call intellivim#core#run#RunList()
    endif

    if !exists(":RunTest")
        command -nargs=0
            \ RunTest :call intellivim#core#test#RunTest()
    endif

    " }}}

endfunction " }}}

function! intellivim#core#ReloadFile() " {{{
    " Update the contents/state of a file after
    "  we (think) it has been changed externally

    " reload the file
    edit!

    " TODO somehow preserve cursor position?

    " refresh problems
    call intellivim#core#Update()
endfunction " }}}

function! intellivim#core#Update() " {{{
    " for now, basically just update problems
    call intellivim#core#problems#UpdateProblems()
endfunction " }}}

function! intellivim#core#GetDocumentation() " {{{
    " Fetch and show the documentation for the element under the cursor

    if !intellivim#InProject()
        return
    endif

    let command = intellivim#NewCommand("get_documentation")
    let command.offset = intellivim#GetOffset()

    " show documentation window
    call intellivim#display#PreviewWindowFromCommand("[Documentation]", command)

endfunction " }}}

function! intellivim#core#GotoDeclaration() " {{{
    " Fetch and show the documentation for the element under the cursor

    if !intellivim#InProject()
        return
    endif

    let command = intellivim#NewCommand("find_declaration")
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    " intellij offsets start at 0; we start at 1
    let offset = result.result.offset + 1
    let file = result.result.file

    if file != expand("%:p")
        " TODO different file. split? vsp? tabn?
        let openCommand = 'split'
        exe openCommand . ' ' . substitute(file, ' ', '\ ', 'g')
    endif

    exe 'goto ' . offset

endfunction " }}}

function s:ShouldIgnoreFiletype(ft) " {{{

    if -1 != index(s:alwaysIgnoredFiletypes, a:ft)
        return 1
    endif

    " TODO user-specifiable
    return 0
endfunction " }}}

" vim:ft=vim:fdm=marker
