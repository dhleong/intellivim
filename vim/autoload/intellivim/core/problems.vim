" Author: Daniel Leong
"

function! intellivim#core#problems#UpdateProblems() " {{{

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

function! intellivim#core#problems#FixProblem() " {{{
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

    if !has_key(result, "result") 
        call intellivim#util#EchoError("No problems under the cursor")
        return
    endif

    if !len(result.result)
        call intellivim#util#Echo("No fixes available")
        return
    endif

    call s:ShowQuickfixWindow(result.result)
    call intellivim#util#Echo("Total results: " + len(result.result))
endfunction " }}}

function! intellivim#core#problems#PromptFix(fix, ...) " {{{
    " Show the prompt to disambiguate a quickfix

    let extras = a:0 ? a:1 : {}
    let returnWinNr = get(extras, 'returnWinNr', bufwinnr('%'))
    let fix = a:fix
    let config = {
            \ 'title': fix.description,
            \ 'list': fix.choices,
            \ 'onSelect': function("s:OnPerformFix"),
            \ 'selectArgs': [returnWinNr, fix.id],
            \ }
    for key in keys(extras)
        let config[key] = extras[key]
    endfor
    call intellivim#display#PromptList(config)
endfunction " }}}

function s:ExecuteQuickFix() " {{{
    let line = getline('.')
    let parts = split(line, ':')
    if len(parts) == 1
        " nothing to be done
        return
    endif

    let fixId = parts[0]
    let fix = s:FindFixById(fixId)
    if type(fix) != type({})
        return
    endif

    let oldWinr = bufwinnr(b:last_bufno)

    " first, make sure it doesn't have choices
    let choices = get(fix, "choices", 0)
    if has_key(fix, 'choices')
        let fixes = b:quickfix_results
        norm! ZZ
        call intellivim#core#problems#PromptFix(fix, {
                \ 'returnWinNr': oldWinr,
                \ 'onCancel': function("s:OnFixPromptCanceled"),
                \ 'cancelArgs': [oldWinr, fixes]
                \ })
        return
    endif

    " no choices; just close tempwindow and perform fix
    norm! ZZ
    call s:OnPerformFix(oldWinr, fixId)
endfunction " }}}

"
" Callbacks
"

function s:OnFixPromptCanceled(oldWinr, fixes) " {{{
    " pop back so we can 
    exe a:oldWinr . 'winc w'

    call s:ShowQuickfixWindow(a:fixes) 
endfunction " }}}

function s:OnPerformFix(oldWinr, fixId, ...) " {{{
    " Perform the chosen fix for the given window
    " Optional Argument:
    "  - arg Argument parameter for the fix

    exe a:oldWinr . 'winc w'

    let command = intellivim#NewCommand("quickfix")
    let command.fixId = a:fixId

    if a:0
        let command.arg = a:1
    endif

    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    call intellivim#core#ReloadFile()

endfunction " }}}

"
" Private utils
"

function s:FindFixById(fixId) " {{{
    let fixes = b:quickfix_results
    for fix in fixes
        if fix.id == a:fixId
            return fix
        endif
    endfor

    return 0
endfunction " }}}

function s:ShowQuickfixWindow(fixes) " {{{

    " prepare contents
    let contents = []
    for quickfix in a:fixes
        call add(contents, quickfix.id . ": " . quickfix.description)
    endfor

    " show quickfix window (not to be confused with vim's quickfix)
    call intellivim#display#TempWindow("[Quick Fix]", contents)
    let b:quickfix_results = a:fixes
    nnoremap <buffer> <cr> :call <SID>ExecuteQuickFix()<cr>

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
