" Author: Daniel Leong
"

function! intellivim#core#Setup() " {{{

    augroup intellivim_core
        autocmd!
        autocmd BufWritePost <buffer> call intellivim#core#Update()
    augroup END

    setlocal omnifunc=intellivim#core#lang#CodeComplete

    " also, update now
    call intellivim#core#Update()
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

" vim:ft=vim:fdm=marker
