{white}Line Processors
They are like commands but affect or block the processing of the entire line
All line processors start with an {gold:#} and require a {gold::} after their name (eg. #name:some text)
#example:#comment: This won't appear, but it's a good example

{white}Raw Line Processor
This line processor stops any command or line processor from executing on this line
#example:#raw:{var:some_var} will not get parsed
#example:#raw:{red}formatting commands are not parsed but §s§ccolor§t codes still function
#example:#raw:#comment:this is still visible

{white}Include Line Processor
Use it to reduce repetition across similar tooltips
{color_note}Included files are evaluated with the same arguments as the calling file
Usage example: "#include:modid:resource/path"
#example:#include:gtnhlib:showcase-include

{white}Boolean If Line Processor
Usage: #if:bool_var:some text
#example:#if:true:This appears because true is true
#example:#if:false:This doesn't appear because false is not true
#example:#if-not:true:This doesn't appear because true is not false
#example:#if-not:false:This appears because false is false

{white}If Defined Line Processor
#example:#ifdef:existing_var:This appears because existing_var is defined
#example:#ifdef:missing_var:This doesn't appear because missing_var is not defined
#example:#ifndef:existing_var:This doesn't appear because existing_var is defined
#example:#ifndef:missing_var:This appears because missing_var is not defined

{white}Line Processor Chaining
Some line processors can be chained:
#example:#if:true:#include:gtnhlib:showcase-include
#example:#if:false:#include:gtnhlib:showcase-include
