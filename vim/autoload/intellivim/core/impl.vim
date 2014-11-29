" Author: Daniel Leong
"

function! intellivim#core#impl#ShowImplementables()

    if !intellivim#InProject()
        return
    endif

    let command = intellivim#NewCommand("get_implementables")
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    let signatures = map(result.result, 'v:val.description')
    call intellivim#display#TempWindow('[Implement]', signatures)

    set ft=java
    nnoremap <buffer> <cr> :call <SID>DoImplement()<cr>
    vnoremap <buffer> <cr> :call <SID>DoImplement()<cr>

endfunction

function! s:DoImplement() range
    let signatures = []
    for i in range(a:firstline, a:lastline)
        call add(signatures, getline(i))
    endfor

    " pop back
    winc p

    " implement
    let command = intellivim#NewCommand("implement")
    let command.offset = intellivim#GetOffset()
    let command.signatures = signatures
    let result = intellivim#client#Execute(command) 
    if intellivim#ShowErrorResult(result)
        " I guess pop back to the temp window
        silent winc p
        return
    endif

    " refresh and re-request
    call intellivim#core#ReloadFile()
    call intellivim#core#impl#ShowImplementables()
endfunction

" vim:ft=vim:fdm=marker
