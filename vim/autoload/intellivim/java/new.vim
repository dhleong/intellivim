" Author: Daniel Leong
"
" Commands for creating new Java classes

" Script Varables {{{
  let s:java_types = ['class', 'abstract', 'interface', 'enum', '@interface']
" }}}

function! intellivim#java#new#Create(type, name) " {{{
    let command = intellivim#NewCommand("java_new")
    let command.type = a:type
    let command.name = a:name
    let result = intellivim#client#Execute(command)

    if intellivim#ShowErrorResult(result)
        return
    endif

    call intellivim#core#find#OpenLocationResult(result.result)

endfunction " }}}

function! intellivim#java#new#CommandComplete(argLead, cmdLine, cursorPos) " {{{
  let cmdLine = strpart(a:cmdLine, 0, a:cursorPos)
  let args = split(cmdLine, '[^\\]\s\zs')
  if len(args) < 2 || (len(args) == 2 && cmdLine !~ '\s$')
    " Suggest java types
    let arg = substitute(a:argLead, '@', '\\@', '')
    return filter(copy(s:java_types), 'v:val =~ "^' . arg . '"')
  endif

  if len(a:argLead) <= 3 || a:argLead =~ '\.'
    " Propose packages
    let current = a:argLead
    let command = intellivim#NewCommand("java_complete_package")
    let command.input = current
    let result = intellivim#client#Execute(command)
    if has_key(result, 'result')
        return result.result
    endif
  endif

  return []
endfunction " }}}

" vim:ft=vim:fdm=marker
