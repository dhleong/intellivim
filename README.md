IntelliVim [![Build Status](http://img.shields.io/travis/dhleong/intellivim.svg?style=flat)](https://travis-ci.org/dhleong/intellivim)
==========

IntelliVim aims to expose the features from [IntelliJ](https://www.jetbrains.com/idea/) inside of [Vim](http://www.vim.org/),
in the spirit of [Eclim](http://eclim.org).


## Features

While IntelliVim is still in the very early stages of development, and most features
should still be assumed to be at the "proof of concept" stage, it does have a few tricks up its sleeve already.

### Problem detection

Problems are marked with visual signs and added to the buffer's location list. Sometimes
they can be fixed with `:FixProblem`.

### Autocomplete

Autocomplete is bound to omnifunc (`<c-x><c-o>`) and should work with [YouCompleteMe](https://github.com/Valloric/YouCompleteMe) out of the box.

### Commands

`:Implement`  Generate method implementations/overrides. 
Press `enter` on a method to implement it, and `q` to quit. 
Visual selection to implement multiple at a time is supported.

`:GotoDeclaration`  Jump to the declaration of the element under the cursor.
A split is opened if the declaration is in another file

`:GetDocumentation`  Display the documentation for the element under the cursor
in a preview window.

`:FixProblem`  Provide options for fixing the problem under the cursor.
Press `enter` on the desired fix to attempt it. "Import Class" usually works
for unambiguous imports, but other fixes are not thoroughly tested yet.

`:JavaOptimizeImports`  Attempts to automatically add imports and organize them.
Handling of ambiguous imports is currently undefined.

#### Mappings

With the exception of setting the `omnifunc` per-buffer, IntelliVim does not come with
any mappings by default. Feel free to use mine:

```vim
" 'java implement'
nnoremap <leader>ji :Implement<cr>
" 'java correct'
nnoremap <leader>jc :FixProblem<cr>
" 'fix imports'
nnoremap <leader>fi :JavaOptimizeImports<cr>
nnoremap gd :GotoDeclaration<cr>
nnoremap K :GetDocumentation<cr>
```


## Other Editors

There is no plan to offer first-party support for any editor besides Vim. That said,
any editor that can make HTTP requests should be able to integrate with the server
component that runs inside IntelliJ. Commands are `POST`'d to the server as a JSON
object that gets inflated directly into an instance of `ICommand` and `execute()`'d.
The result is returned as a JSON object with an `error` key if anything went wrong,
else a `result` key containing whatever expected result for the command.