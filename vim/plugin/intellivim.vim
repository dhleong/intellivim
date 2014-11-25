" Author: Daniel Leong
"
" Description: {{{
"   Plugin to provide integration with IntelliJ in
"   the spirit of eclim.
" }}}

function! s:Setup()
    if !intellivim#InProject()
        " not in a project? don't bother
        return
    endif

    " setup shared functionality across filetypes
    call intellivim#core#Setup()

    " filetype-specific setup
    let filetype = &ft;
    let setupFunction = "intellivim#" . filetype . "#Setup"
    if exists(setupFunction)
        exe "call " . setupFunction . "()"
    endif
endfunction

augroup eclim_setup
    autocmd!
    autocmd BufReadPre * call <SID>Setup()
    autocmd BufNewFile * call <SID>Setup()
augroup END
