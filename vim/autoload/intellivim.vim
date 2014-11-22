
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
        let fileDir = expand(expandStr)
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

function! intellivim#Setup()
    let project = intellivim#GetCurrentProject()
    if empty(project)
        " no project, no setup
        return
    endif

    " TODO commands, etc.
    call intellivim#core#Setup()
endfunction

function! intellivim#ShowErrorResult(result) " {{{
    if has_key(a:result, 'error')
        " TODO intellivim#util#EchoError
        redraw " prevent 'press enter to continue'
        echo a:result.error
        return 1
    endif

    " success!
    return 0
endfunction " }}}

" vim:ft=vim:fdm=marker
