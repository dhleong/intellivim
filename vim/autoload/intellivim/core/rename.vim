" Author: Daniel Leong
"

function! intellivim#core#rename#RenameElement(newName) " {{{
    if !intellivim#InProject()
        call intellivim#ShowErrorResult("No project found")
        return 
    endif

    " TODO allow no arg and use fancy prompt for rename
    let command = intellivim#NewCommand("rename_element")
    let command.offset = intellivim#GetOffset()
    call s:DoRename(command, a:newName)
endfunction " }}}

"
" Callbacks
"

function! s:DoRename(command, newName) " {{{

    " make sure everything's written
    "  so no work gets lost
    call intellivim#SilentUpdate()
    wall

    let command = a:command
    let command.rename = a:newName
    let result = intellivim#client#Execute(command)
    if intellivim#ShowErrorResult(result)
        return
    endif

    let myWinNr = winnr()

    try
        for changed in result.result.changed
            " silently reload changed buffers
            let winnr = bufwinnr(changed)
            if winnr != -1
                exe winnr . "winc w"
                call intellivim#core#ReloadFile()
            endif
        endfor

        for [fromPath, toPath] in items(result.result.renamed)
            " silently swap buffers for windows
            "  pointing to old paths
            let winnr = bufwinnr(fromPath)
            if winnr != -1
                exe winnr . "winc w"
                let bufnr = bufnr('%')
                enew
                exe 'bdelete ' . bufnr
                exe 'edit ' . escape(toPath, ' ')
            endif
        endfor

    finally
        exe myWinNr . 'winc w'
    endtry
endfunction " }}}

" vim:ft=vim:fdm=marker
