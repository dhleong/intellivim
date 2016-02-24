" Author: Daniel Leong
"

let s:ivroot = expand("<sfile>:p:h:h:h")

"
" Internal util
"

function! s:detectServer() " {{{
    let path = $HOME . "/.intellivim/.instances/"
    let instances = glob(path . '*', 1, 1)
    call sort(instances, 'n')
    for i in instances
        let raw = join(readfile(i), '')
        if raw !~ '^{'
            continue
        endif

        if exists('*json_decode')
            let data = json_decode(raw)
        else
            let data = eval(raw)
        endif

        if '' == get(data, 'port', '')
            continue
        endif

        if s:getVersion() != get(data, 'version', '')
            continue
        endif

        " NB: for now, the first-matched is fine
        return data
    endfor

    return {'port': -1}
endfunction " }}}

function! s:getVersion() " {{{
    let existing = get(g:, 'intellivim#version', '')
    if existing != ''
        return existing
    endif

    let path = s:ivroot . '/src/main/resources/META-INF/plugin.xml'
    try
        let contents = readfile(path)
    catch /E484/
        " couldn't find the file (for whatever reason)
        return '(nofile)'
    endtry

    let versionLine = matchlist(contents, '.*<version>\(.*\)</version>.*')
    if len(versionLine) == 0
        return '(noversion)'
    endif

    " cache for later
    let g:intellivim#version = versionLine[1]
    return versionLine[1]
endfunction " }}}

"
" Public commands
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

    let imls = split(iml, '\n')
    if len(imls) == 0
        " no project found
        return ""
    endif

    let iml = imls[0]
    let b:intellivim_project = iml
    return iml
endfunction " }}}

function! intellivim#InProject() " {{{
    return !empty(intellivim#GetCurrentProject())
endfunction " }}}

function! intellivim#IsRunning() " {{{
    " TODO cache the server info?
    return s:detectServer().port > 0
endfunction " }}}

function! intellivim#NewCommand(commandName) " {{{
    " Convenience to prepare basic command object
    "  of given command name, filled with the
    "  current file and project info

    " detect the right port (if any)
    " TODO cache this?
    let b:intellivim_port = s:detectServer().port

    let project = intellivim#GetCurrentProject()
    let projectDir = fnamemodify(project, ':h') " does not contain trailing slash
    let fileFull = expand('%:p')
    let file = strpart(fileFull, len(projectDir) + 1)
    return {
        \ 'client': 'vim',
        \ 'v': s:getVersion(),
        \ 'exe': exepath(v:progpath),
        \ 'instance': v:servername,
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
        redraw " prevent 'press enter to continue'
        call intellivim#util#EchoError(a:result.error)
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
