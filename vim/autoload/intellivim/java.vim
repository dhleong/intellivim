" Author: Daniel Leong
"

function! intellivim#java#Setup() " {{{

    " define commands {{{
    if 2 != exists(":JavaNew")
        command -nargs=+
            \ -complete=customlist,intellivim#java#new#CommandComplete
            \ JavaNew :call intellivim#java#new#Create(<f-args>)
    endif

    if !exists(":JavaOptimizeImports")
        command -nargs=0 JavaOptimizeImports
            \ call intellivim#java#OptimizeImports()
    endif
    " }}}

endfunction " }}}

function! intellivim#java#Generate() " {{{

    let command = intellivim#NewCommand("java_generate")
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    if !has_key(result, 'result') || !type(result.result) == type([])
        " Nothing to do
        call intellivim#util#Echo("Nothing to generate here")
        return
    endif

    let generateActions = result.result

    call intellivim#display#TempWindow('[Generate...]', generateActions)
    nnoremap <buffer> <cr> :call <SID>DoGenerate()<cr>
endfunction " }}}

function! intellivim#java#OptimizeImports() " {{{

    let command = intellivim#NewCommand("java_import_optimize")
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    call intellivim#core#ReloadFile(result)

    if !has_key(result, 'result') || !type(result.result) == type([])
        " all unambiguous
        return
    endif

    " fix ambiguousness
    let fixes = result.result
    if len(fixes) == 0
        " this *shouldn't* happen if we get here,
        "  but just in case....
        return
    endif

    " prepare initial resolution and fire
    let b:intellivim_pending_fixes = fixes
    let b:intellivim_last_fix_index = -1
    call s:OnContinueImportResolution()

endfunction " }}}

"
" Callbacks
"

function! s:DoGenerate() " {{{
    let action = getline(line('.'))

    " close the temp window
    norm! ZZ

    " TODO perform; also, we probably will
    "  need to provide input somehow...
    call intellivim#util#Echo("TODO: generate " . action)
endfunction " }}}

function! s:OnContinueImportResolution() " {{{
    let fixes = b:intellivim_pending_fixes
    let index = b:intellivim_last_fix_index + 1
    if index >= len(fixes)
        " we're done!
        unlet b:intellivim_pending_fixes
        unlet b:intellivim_last_fix_index
        return
    endif

    let b:intellivim_last_fix_index = index
    call intellivim#core#problems#PromptFix(fixes[index], {
            \ 'returnWinNr': bufwinnr('%'),
            \ 'onDone': function("s:OnContinueImportResolution")
            \ })
endfunction " }}}

" vim:ft=vim:fdm=marker
