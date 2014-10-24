PDF html and text extraction 
===

Based on Mozilla's pdf.js.


```xml
<svg:tspan 
    font-family="g_font_2" font-size="11.9552px" y="0" fill="rgb(0,0,0)"
    x="0 4.5573950472 11.0377449208 17.5180826002 24.3585898079 30.2437009137 34.8063713961 39.3690418785 44.5269512842
      -1 55.687238081 61.8075042647
      -1 72.4903242839
      "
    M="|                                  ^----------------------$  |  {type: pos, labels: {n: nnp}}
       |                                  ^---__________---------$  |  {type: entity}                   "
       >supported by a grant provided by East Piedmont University (U</svg:tspan>
```

* Annotation format (M="...")


+ What's with the funky XML/SVG formatting?
  + Consumers:
    + Human-readable
      Text and annotations are separate, but are easy to read and understand 

    + grep (and general command-line) friendly
      The extracted text appears on lines with little else, so it's easy to extract just the text for search or visual inspections
      
    + Easily machine readable and writeable
      The annotations are easy to parse, easy to add and remove

    + Browser renderable
      The original PDF content is accurately rendered in a browser 

    + Augmentable (annotation) 
      
    + Annotations can be readily merged/split
    
    + (Relatively) insensitive to accidental whitespace changes
      + or, at least, easily correctable
      
      
