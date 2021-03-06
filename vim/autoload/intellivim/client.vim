" Author: Daniel Leong
"

if !has('python') && !has('python3')
    echo 'intellivim requires python support'
    finish
endif

let s:cwdir = expand("<sfile>:p:h")
let s:script = s:cwdir . '/client.py'
if has('python')
  execute 'pyfile ' . fnameescape(s:script)
elseif has('python3')
  execute 'py3file ' . fnameescape(s:script)
endif

function! intellivim#client#Execute(command, ...)
    " Options:
    "  timeout (default: 1.5): Timeout in seconds

    let timeout = a:0 > 0 ? a:1 : -1

python << PYEOF
command = vim.eval('a:command')
timeout = float(vim.eval('timeout'))
if timeout == -1:
    timeout = None

result = IVClient.execute(command, timeout)
if result is None:
    vim.command("let result = ''")
else:
    vim.command("let result = '%s'" % result.replace("'", "''"))
PYEOF

    if empty(result)
        return {}
    else
        return eval(result)
    endif
endfunction
