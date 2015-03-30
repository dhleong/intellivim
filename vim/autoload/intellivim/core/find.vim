" Author: Daniel Leong
"

function! intellivim#core#find#Declaration() " {{{
    " Find the declaration of the thing under the cursor

    let command = intellivim#NewCommand("find_declaration")
    call s:Find(command, "declaration of " . expand("<cword>"))
endfunction " }}}

function! intellivim#core#find#Implementations() " {{{
    " Find implementations of the thing under the cursor

    let command = intellivim#NewCommand("find_implementations")
    call s:Find(command, "implementations of " . expand("<cword>"))
endfunction " }}}

function! intellivim#core#find#Usages() " {{{
    " Find usages of the thing under the cursor

    let command = intellivim#NewCommand("find_usages")
    call s:Find(command, "usages of " . expand("<cword>"))
endfunction " }}}

"
" Private util
"

function! s:Find(command, type) " {{{

    if !intellivim#InProject()
        return
    endif

    let command = a:command
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    let found = result.result
    let type = type(found)
    let isList = type == type([])
    if type == type(0) || (isList && !len(found))
        call intellivim#util#EchoError("No " . a:type . " found")
        return
    elseif isList && len(found) == 1
        call s:OpenLocationResult(found[0])
    elseif !isList
        call s:OpenLocationResult(found)
    else
        call s:OpenQuickFix(found)
    endif

endfunction " }}}

function! s:OpenLocationResult(result) " {{{

    " clear the quickfix
    call intellivim#display#ClearQuickFix()

    " intellij offsets start at 0; we start at 1
    let offset = a:result.offset + 1
    let file = a:result.file

    if file != expand("%:p")
        " TODO different file. split? vsp? tabn?
        let openCommand = 'split'
        exe openCommand . ' ' . substitute(file, ' ', '\ ', 'g')
    endif

    exe 'goto ' . offset

endfunction " }}}

function! s:OpenQuickFix(locationItems) " {{{
    " map to quickfix items, first
    let qfItems = map(a:locationItems, '{
            \ "filename": v:val.file,
            \ "lnum": byte2line(v:val.offset + 1),
            \ "col": v:val.offset - byte2line(v:val.offset + 1) + 1
            \ }')

    " and... set
    call setqflist(qfItems, 'r')
    copen
endfunction " }}}

" vim:ft=vim:fdm=marker
