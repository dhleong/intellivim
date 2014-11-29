" Author: Daniel Leong
"

" Variables {{{
" never run core Setup in these filetypes
let s:alwaysIgnoredFiletypes = ["gitcommit"]
" }}}

function! intellivim#core#Setup() " {{{

    if &previewwindow || &ft == '' || s:ShouldIgnoreFiletype(&ft)
        " preview window, or no real ft; don't do anything
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
            \ call intellivim#core#FixProblem()
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
    let command = intellivim#NewCommand("get_problems")
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result, 1)
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

function! intellivim#core#FixProblem() " {{{
    " Begin the process of fixing the problem under the cursor
    if !intellivim#InProject()
        return
    endif

    let command = intellivim#NewCommand("get_fixes")
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    " prepare contents
    let contents = []
    for quickfix in result.result
        call add(contents, quickfix.id . ": " . quickfix.description)
    endfor

    " show quickfix window (not to be confused with vim's quickfix)
    call intellivim#display#TempWindow("[Quick Fix]", contents)
    nnoremap <buffer> <cr> :call <SID>ExecuteQuickFix()<cr>

endfunction " }}}

function s:ExecuteQuickFix() " {{{
    let line = getline('.')
    let parts = split(line, ':')
    if len(parts) == 1
        " nothing to be done
        return
    endif

    let fixId = parts[0]
    let oldWinr = bufwinnr(b:last_bufno)

    " close the tempwindow and pop back
    norm! ZZ
    exe oldWinr . 'winc w'

    let command = intellivim#NewCommand("quickfix")
    let command.fixId = fixId
    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    call intellivim#core#ReloadFile()

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

function s:ShouldIgnoreFiletype(ft) " {{{

    if -1 != index(s:alwaysIgnoredFiletypes, a:ft)
        return 1
    endif

    " TODO user-specifiable
    return 0
endfunction " }}}

" vim:ft=vim:fdm=marker
