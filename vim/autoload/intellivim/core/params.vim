" Author: Daniel Leong
" 
"   Parameter hints module for IntelliVim
"

let s:defaultTriggers = ['(', ')', ',']

function! intellivim#core#params#Setup()
    " augroup intellivim_core_params_init
    "     autocmd!
    "     autocmd InsertEnter <buffer> call <SID>Restart()
    " augroup END
    call s:Restart()
endfunction


"
" Private utils
"

function! s:IsParamsTrigger(char)
    " we could allow extensions for other languages
    "  that don't use `function(arg, arg)` syntax
    let triggers = s:defaultTriggers
    return -1 != index(triggers, a:char)
endfunction

function! s:Restart()
    augroup intellivim_core_params
        autocmd!
        " NB: ideally we'd show after a delay using CursorHoldI,
        "  but for whatever reason it doesn't work consistently....
        " autocmd CursorMovedI <buffer> call <SID>PrepareDelay()
        autocmd CursorMovedI <buffer> call <SID>TriggerHints()
    augroup END
endfunction

" function! s:PrepareDelay()
"     augroup intellivim_core_params
"         autocmd!
"         autocmd CursorHoldI <buffer> call <SID>TriggerHints()
"     augroup END
" endfunction

function! s:TriggerHints()
    " disable temporarily
    augroup intellivim_core_params
        autocmd!
    augroup END

    " restart listener
    call s:Restart()

    " TODO do nothing if disabled

    let col = col('.')
    if col <= 1
        " nothing before us
        return
    endif

    let lastTyped = getline('.')[col - 2]
    if !s:IsParamsTrigger(lastTyped)
        return
    endif

    " make sure the file on disk is up to date
    call intellivim#SilentUpdate()

    let command = intellivim#NewCommand("get_param_hints")
    let command.offset = intellivim#GetOffset()
    call intellivim#display#PreviewWindowFromCommand("[Params]", command)

    return ''
endfunction

" vim:ft=vim:fdm=marker
