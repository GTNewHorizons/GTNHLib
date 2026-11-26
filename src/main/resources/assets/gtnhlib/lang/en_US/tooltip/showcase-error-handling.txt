{white}Command error styling has been unified
#example:{if}
#example:{if-false}
#example:{ifdef}
#example:{ifndef}
#example:{color-pattern}
#example:{item:not:real#0}
#example:{fluid:not_real}
#example:{if:missing_var:asd}
#example:{if:some_text:asd}

{white}Unknown commands don't crash the tooltip
#example:some text {missing-command:ASD} more text {missing-command-no-arg} even more text

{white}Bracket issues are highlighted correctly
#example:some text {green:{italic} more text
#example:some text {green more text

{white}Unknown Line Processor Don't Crash the Tooltip
#example:#bad-line-processor
#example:#bad-line-processor:

{white}Line processor errors are all formated similarly
#example:#if:
#example:#if-not:
#example:#ifndef:
#example:#ifdef:
#example:#if:missing_var:
#example:#if:some_text:
#example:#include:gtnhlib:showcase-error-handling
#example:#include:gtnhlib:file-that-does-not-exist
