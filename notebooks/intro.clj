^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns intro
  (:require
            [nextjournal.clerk :as clerk]
            [flow-storm.clerk :as clerk-storm]))

;; # Modeling Chess: Introduction
;;

^{::clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:ol.border-b
  [:li [:a {:href "../../"} "Welcome"]]
  [:li [:strong "Introduction"]]
  [:li [:a {:href (clerk/doc-url "notebooks/walkthrough")} "Code Walkthrough"]]
  [:li [:a {:href (clerk/doc-url "notebooks/closing")} "Closing Thoughts"]]])

;; ### Background story
;;
;; * I was helping undergraduate students with a coding exercise about modeling
;; the game of Chess. Many of them getting trapped in class-based OO
;; design (using Java or Kotlin). Days later:
;;
;; * I was reading _"Software Design for
;; Flexibility_".[^sdff_book] Coincidentally, chapter _2.4 "Abstracting a
;; Domain"_ models the game of Checkers. It then proceeds to abstract a core
;; domain, and leaves the implementation of the rules of Chess as an exercise.
;; [^sdff_book]: by Chris Hanson and Gerald Jay Sussman ![image](https://mit-press-new-us.imgix.net/covers/9780262045490.jpg?auto=format&w=298&dpr=2&q=80)
;;
;; Soon after, I found myself working on the exercise, hoping to achieve a
;; simple solution using basic functional programming concepts. These notebooks
;; are my attempt at documenting it.
;;
;; The goal of this exercise is pedagogical: to learn software design concepts,
;; touching upon the basics of topics such us:
;;
;; * Functional programming
;; * Immutability
;; * Literate programming
;; * Interactive (and "moldable") development
;; * Data-orientation
;; * Stratified design
;; * Simplicity
;; * some Testing techniques
;; * and some Clojure
;;
;; ### Why Clojure? I wished you used _&lt;JavaScript/Python/...&gt;_
;;
;; Why not? Clojure is a simple functional language, ideal for my goal... and I
;; like it 🙂. If you don't know Clojure:
;;
;;   * I hope the code is not hard to follow. I use only basic features of the
;; language, and no external library depedencies. This is not a tutorial on
;; Clojure (yet?) but I try to explain the code as I go
;;   * Hey, this might be a good opportunity for you to learn it!
;;   * If you don't want to use Clojure, that's fine too: I'd encourage you to
;; re-implement this exercise in your favorite language as you follow the code
;; walkthrough.
;;
;; ### "Hello World" in Clojure
;;
;; If you are new to Lisp languages, the biggest syntax difference is in the way
;; you call a function:[^lisp_simplification]
;; * in C/JavaScript-like syntax: `functionName(arg1, arg2, ...)`
;; * in Lisp/Clojure syntax: `(function-name arg1 arg2 ...)`
;; [^lisp_simplification]: an over-simplification of course, but not a lie
;;
;; Let's define a function `hello-message` that receives a `name` (string) and returns a `"Hello ... !"` message (string):
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(defn hello-message [name]
  (str "Hello " name "!"))

;; Its JavaScript equivalent would be:
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/code {::clerk/opts {:language "javascript"}} "function helloMessage(name) {
   return \"Hello \" + name + \"!\";
}")





;; Here's an expression that calls the above function passing `"World"`, which evaluates to, you guessed it, `"Hello World!"`:
^{:nextjournal.clerk/visibility {:code :show :result :show}}
(hello-message "World")

;; ### Code-stepping debugger
;;
;; To make some code snippets easier to follow on these notebooks, I worked on a code viewer that let's you step through the code execution (back and forth).[^flowstorm] \
;; \
;; Let's evaluate the following code snippet, which: \
;; 1\. First calls our `hello-message` function passing `"World"`,\
;; 2\. Then repeats the result of that invocation ten times, which evaluates to a Clojure _sequence_:
;; [^flowstorm]: Based on the (amazing!) [FlowStorm](https://www.flow-storm.org/) by [jpmonettas](https://github.com/sponsors/jpmonettas) ![](https://github.com/flow-storm/flow-storm-debugger/raw/master/docs/images/icon.png)

#_ "I'm sorry if you favorite language doesn't have something like it. Just wait... every good thing from Lisp/Clojure finds its way into other languages, only decades later ;-)"

^{::clerk/visibility {:code :hide :result :show}
  ::clerk/width :wide
  ::clerk/auto-expand-results? true
  ::clerk/viewer clerk-storm/timeline-stepper-viewer}
(clerk-storm/show-trace {:include-fn-names []}
  (repeat 10 (hello-message "World"))
  #_(let [msg (hello-message "World")]
    (repeat 10 msg)))


#_(comment

;;
;; ### Code organization
;;
;; The code is organized in the following namespaces:
;;
^{::clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:ul
  [:li [:a {:href (clerk/doc-url "src/boardgames/core")} "boardgames.core: "] [:span "Generic domain to model board games"]]
  [:li [:a {:href (clerk/doc-url "src/boardgames/chess")} "boardgames.chess: "] [:span "Implementation of classic Chess"]]
  [:li [:a {:href (clerk/doc-url "src/boardgames/checkers")} "boardgames.checkers: "] [:span "Implementation of Checkes (TBD)" ]]
  [:li [:a {:href (clerk/doc-url "src/boardgames/ttt")} "boardgames.ttt: "] [:span "Implementation of Tic-Tac-Toe (TBD)"]]
  [:li [:a {:href (clerk/doc-url "src/boardgames/clerk_viewers")} "boardgames.clerk-viewers: "] [:span "The graphical widgets to visualize boards and moves on Clerk notebooks"]]
  ])
)
#_
^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 (clerk/use-headers [["namespace" "description"]
                     [(clerk/html [:a {:href (clerk/doc-url "src/boardgames/core")} "boardgames.core"]) "Generic domain to model board games"]
                     [(clerk/html [:a {:href (clerk/doc-url "src/boardgames/chess")} "boardgames.chess"]) "Implementation of classic Chess"]
                     [(clerk/html [:a {:href (clerk/doc-url "src/boardgames/checkers")} "boardgames.checkers"]) "Implementation of Checkes (TBD)"]
                     [(clerk/html [:a {:href (clerk/doc-url "src/boardgames/ttt")} "boardgames.ttt"]) "Implementation of Tic-Tac-Toe (TBD)"]
                     [(clerk/html [:a {:href (clerk/doc-url "src/boardgames/clerk-viewers")} "boardgames.clerk-viewers"]) "The graphical widgets to visualize boards and moves on Clerk notebooks"]]))






;; ### Let's GO! 🏁
;;
^{::clerk/visibility {:code :hide :result :show}}
(clerk/html [:p "Enough talk! Jump to the " [:a {:href (clerk/doc-url "notebooks/walkthrough")} "code walkthrough"] " of the implementation."])
