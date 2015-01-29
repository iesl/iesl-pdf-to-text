PDF to XML converter 
===

Based on Mozilla's pdf.js.

+ SVG expanded syntax      
      
Convert PDF to SVG
------------------
```bash
  bin/run.js --svg -i /path/to/input.pdf -o /path/to/output.svg
```

Convert PDF to SVG and extract tspan text
-----------------------------------------
```bash
  bin/inspect.sh /path/to/input.pdf /path/to/output.svg /path/to/output.txt
```

Test
----
```bash
 sbt test 
```
