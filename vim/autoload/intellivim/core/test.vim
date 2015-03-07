" Author: Daniel Leong
"

let s:test_index_var = "test_index"
let s:test_root_var = "test_root"
let s:indent_amount = 2

" TODO make these a proper settings
let g:IVTestNotRunHighlight = "MoreMsg"
let g:IVTestRunningHighlight = "Label"
let g:IVTestPassedHighlight = "incsearch"
let g:IVTestFailedHighlight = "Constant"
let g:IVTestErrorHighlight = "Error"

" Python functions {{{
" Requiring python is gross, but it's the only way to append to
"  a buffer that isn't visible, and that is surely required
function! s:renderJunit(bufno) " {{{
    if !has('python')
        return
    endif
    
    " prepare vars so python can pick them up
    let bufno = a:bufno
    let rootVar = s:test_root_var
    let indexVar = s:test_index_var
    let indentAmount = s:indent_amount

py << PYEOF
import vim
bufno = int(vim.eval('bufno')) # NB int() is crucial
rootVar = vim.eval('rootVar')
indexVar = vim.eval('indexVar')
indentAmount = int(vim.eval('indentAmount'))
buf = vim.buffers[bufno]
if buf:
    buf.options['readonly'] = False
    buf.options['modifiable'] = True

    oldEnd = len(buf)

    # clear the buffer first
    buf[:] = None

    index = buf.vars[indexVar]

    def renderNode(node, indent=0):
        nodeId = node['id']
        kids = node['kids']

        # use the node from the index
        node = index[nodeId]
        spaces = ' ' * indent
        name = node['name']
        state = "NOT_RUN"
        if node.has_key('state'):
            state = node['state']

        buf.append("[%3s]%s%s (%s)" % (nodeId, spaces, name, state))

        if node.has_key('output'):
            spaces += ' ' * indentAmount
            lines = node['output'].split('\r')
            lines = map(lambda l: "%s%s" % (spaces, l), lines)
            lines = [''] + lines + [''] # pad the output
            buf.append(lines)

            # TODO folding for output on PASSED nodes?

        for kid in kids:
            renderNode(kid, indent + indentAmount)

    # render recursively
    root = buf.vars[rootVar]
    renderNode(root)

    buf.options['readonly'] = True
    buf.options['modifiable'] = False

PYEOF

    " NB this is a bit wacky, but it will force the window 
    "  to re-render, when `redraw!` would not
    call intellivim#display#ScrollBuffer(bufno, 'end')
    call intellivim#display#ScrollBuffer(bufno, 'start')

    " TODO restore cursor position somehow?
endfunction " }}}
" }}}

"
" Public functions
"

function! intellivim#core#test#RunTest() " {{{
    " attempt to run appropriate test under cursor
    let command = intellivim#NewCommand("run_test")
    let command.offset = intellivim#GetOffset()
    let result = intellivim#client#Execute(command)
    call intellivim#ShowErrorResult(result)
endfunction " }}}

"
" Callbacks
"

function! intellivim#core#test#onPrepareOutput(launchId) " {{{
    let bufno = intellivim#core#run#onPrepareOutput(a:launchId, ["Preparing " . a:launchId . "..."])

    " pop back to setup syntax highlighting
    winc p

    call s:declareStatefulSyntaxGroup("NOT_RUN", g:IVTestNotRunHighlight)
    call s:declareStatefulSyntaxGroup("RUNNING", g:IVTestRunningHighlight)
    call s:declareStatefulSyntaxGroup("PASSED", g:IVTestPassedHighlight)
    call s:declareStatefulSyntaxGroup("FAILED", g:IVTestFailedHighlight)
    call s:declareStatefulSyntaxGroup("ERROR", g:IVTestErrorHighlight)

    " and back again
    winc p
    return bufno
endfunction " }}}

function! intellivim#core#test#onCancelled(bufNo) " {{{
    call intellivim#core#run#onCancelled(a:bufNo)
endfunction " }}}

function! intellivim#core#test#onTerminated(bufNo) " {{{
    " ?
endfunction " }}}

function! intellivim#core#test#onStartTesting(bufNo) " {{{

    " ask for the node
    let command = intellivim#NewCommand("get_active_test")
    let command.lazy = 'true'
    let timeout = 3 " slightly longer timeout
    let result = intellivim#client#Execute(command, timeout)

    " use for testing:
    " let result = {
    "     \ "result": {
    "         \ "id": "9",
    "         \ "state": "NOT_RUN",
    "         \ "name": "Test Root Node",
    "         \ "kids": [
    "             \ { 
    "                 \ "name": "Suite1",
    "                 \ "state": "NOT_RUN",
    "                 \ "id": "0",
    "                 \ "kids": [
    "                     \ { "name": "Test1", "id": "1",
    "                         \ "state": "PASSED", "kids": [] }
    "                 \ ]
    "             \ },
    "             \ { "name": "Test4", "id": "4", "state": "ERROR", "kids": [] },
    "             \ { "name": "Test3", "id": "3", "state": "FAILED", "kids": [] },
    "             \ { "name": "Test2", "id": "2", "state": "RUNNING", "kids": [] }
    "         \ ]
    "     \ }
    " \ }
    if intellivim#ShowErrorResult(result)
        return
    endif

    " index all the nodes for faster updates
    let root = result.result
    let index = {}
    let workspace = [root]
    while len(workspace)
        let next = remove(workspace, 0)
        let index[next.id] = next

        for kid in next.kids
            call add(workspace, kid)
        endfor
    endwhile

    let bufno = str2nr(a:bufNo)
    call setbufvar(bufno, s:test_root_var, root)
    call setbufvar(bufno, s:test_index_var, index)
    call s:renderJunit(bufno)

endfunction " }}}

function! intellivim#core#test#onTestOutput(bufNo, nodeId, type, output) " {{{
    
    let bufno = str2nr(a:bufNo)
    let index = getbufvar(bufno, s:test_index_var)
    let node = index[a:nodeId]

    " we're lazy about output type, for now...
    if !has_key(node, "output")
        let node.output = a:output
    else
        let node.output = node.output . "\r" . a:output
    endif

    " just re-render
    call s:renderJunit(bufno)

    " TODO scroll to make output visible?

endfunction " }}}

function! intellivim#core#test#onTestStateChange(bufNo, nodeId, newState) " {{{
    
    let bufno = str2nr(a:bufNo)
    let index = getbufvar(bufno, s:test_index_var, {})
    let node = index[a:nodeId]
    let node.state = a:newState

    " just re-render
    call s:renderJunit(bufno)

    " TODO scroll to make node visible?
endfunction " }}}

"
" Private functions
"

function! s:declareStatefulSyntaxGroup(state, highlight) " {{{

    exe "syntax region " . a:highlight . " matchgroup=Quote" .
        \ " start=/^\\[.*\\]/ end=/[ ](" . a:state . ")\\n/ concealends oneline"

endfunction " }}}

" vim:ft=vim:fdm=marker
