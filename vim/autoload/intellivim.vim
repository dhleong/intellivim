" Author: Daniel Leong
"

function! intellivim#GetOffset() " {{{
    let line = line('.')
    let col = col('.')

    " NB line2byte is 1-indexed
    return line2byte(line) - 1
            \ + col - 1
endfunction " }}}

function! intellivim#GetCurrentProject() " {{{

    if exists('b:intellivim_project')
        " cached for speed
        return b:intellivim_project
    endif

    let expandStr = '%:p:h'
    let fileDir = expand(expandStr)
    let iml = glob(fileDir . '/*.iml')
    while !empty(fileDir) && empty(iml)
        let expandStr = expandStr . ':h'
        let lastFileDir = fileDir
        let fileDir = expand(expandStr)

        if lastFileDir == fileDir
            " at root!
            break
        endif

        let iml = glob(fileDir . '/*.iml')
    endwhile

    let b:intellivim_project = iml
    return iml
endfunction " }}}

function! intellivim#InProject() " {{{
    return !empty(intellivim#GetCurrentProject())
endfunction " }}}

function! intellivim#NewCommand(commandName) " {{{
    " Convenience to prepare basic command object
    "  of given command name, filled with the
    "  current file and project info

    let project = intellivim#GetCurrentProject()
    let projectDir = fnamemodify(project, ':h') " does not contain trailing slash
    let fileFull = expand('%:p')
    let file = strpart(fileFull, len(projectDir) + 1)
    return {
        \ 'project': project,
        \ 'file': file,
        \ 'command': a:commandName
        \ }
endfunction " }}}

function! intellivim#SilentUpdate() " {{{
    silent noautocmd update
endfunction " }}}

function! intellivim#ShowErrorResult(result, ...) " {{{
    " Optional Arg:
    "  reject_empty If truthy, a missing "result" key
    "               WILL be treated as an error
    if has_key(a:result, 'error')
        " TODO intellivim#util#EchoError
        redraw " prevent 'press enter to continue'
        echo a:result.error
        return 1
    endif

    if !has_key(a:result, 'result') && a:0 && a:1
        " not an error, but let's protect ourselves from missing results
        return 1
    endif

    " success!
    return 0
endfunction " }}}

" vim:ft=vim:fdm=marker
