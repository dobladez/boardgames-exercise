(ns boardgames.kingcheck-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [boardgames.utils :as u]
            [boardgames.test-utils :as t]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))

(deftest king-checks-contraints-test

  (t/expect-chess-moves {:piece :P}
                    ;; Cannot move P, it's pinned by r
                    '[[- - - - -]
                      [- - - - -]
                      [K P - r -]
                      [- - - - -]
                      [- - - - -]]

                    '[])
  (t/expect-chess-moves {:piece :K}
                    ;; K cannot move forward
                    '[[- - - - -]
                      [- - - r -]
                      [K - - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -] [- - - - -]
                      [- - - r -] [- - - r -] [- - - r -]
                      [- - - - -] [- - - - -] [- K - - -]
                      [K - - - -] [- K - - -] [- - - - -]
                      [- - - - -] [- - - - -] [- - - - -]])

  (t/expect-chess-moves {:piece :p} ;; lower case sets the turn to black
                    ;; Cannot move p, it's pinned by R
                    '[[- - - - -]
                      [k p - - R]
                      [- - - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[])

  (t/expect-chess-moves {:piece :k}

                    '[[k - - - -]
                      [- - - - -]
                      [P P - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[[- k - - -]
                      [- - - - -]
                      [P P - - -]
                      [- - - - -]
                      [- - - - -]]))


(deftest castling-test

  ;; white. short
  (t/expect-chess-moves {:piece :K}
                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [P P P P P P P P]
                          [R N B Q K - - R]]

                        '[[r n b q k b n r] [r n b q k b n r]
                          [p p p p p p p p] [p p p p p p p p]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [P P P P P P P P] [P P P P P P P P]
                          [R N B Q - K - R] [R N B Q - R K -]])

  (t/expect-chess-moves {:piece :K}
                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [P P P P - P P P]
                          [R N B - K - - R]]

                        '[[r n b q k b n r] [r n b q k b n r] [r n b q k b n r] [r n b q k b n r]
                          [p p p p p p p p] [p p p p p p p p] [p p p p p p p p] [p p p p p p p p]
                          [- - - - - - - -] [- - - - - - - -] [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -] [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -] [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -] [- - - - - - - -] [- - - - - - - -]
                          [P P P P - P P P] [P P P P K P P P] [P P P P - P P P] [P P P P - P P P]
                          [R N B K - - - R] [R N B - - - - R] [R N B - - K - R] [R N B - - R K -]])

  ;; black. short
  (t/expect-chess-moves {:piece :k}
                        '[[r n b q k - - r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [P P P P P P P P]
                          [R N B Q K B N R]]

                        '[[r n b q - k - r] [r n b q - r k -]
                          [p p p p p p p p] [p p p p p p p p]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [P P P P P P P P] [P P P P P P P P]
                          [R N B Q K B N R] [R N B Q K B N R]])

;; white. long
  (t/expect-chess-moves {:piece :K}
                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [P P P P P P P P]
                          [R - - - K B N R]]

                        '[[r n b q k b n r] [r n b q k b n r]
                          [p p p p p p p p] [p p p p p p p p]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [- - - - - - - -] [- - - - - - - -]
                          [P P P P P P P P] [P P P P P P P P]
                          [- - K R - B N R] [R - - K - B N R]]))


(deftest castling-king-check-on-path

  ;; white. short
(t/expect-chess-moves {:piece :K}
                        '[[- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - r - -]
                          [- - - - - - - -]
                          [P P P P P - P P]
                          [R N B Q K - - R]]

                        '[])

  (t/expect-chess-moves {:piece :K}
                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - - - r - -]
                          [- - - - - - - -]
                          [P P P P P - P P]
                          [R N B Q K - - R]]

                        '[])

  (t/expect-chess-moves {:piece :K}
                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - b - - - - -]
                          [- - - - - - - -]
                          [P P P P - P P P]
                          [R N B Q K - - R]]

                        '[])

  (t/expect-chess-moves {:piece :K}
                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - b - - - -]
                          [- - - - - - - -]
                          [P P P P P - P P]
                          [R N B Q K - - R]]

                        '[[r n b q k b n r]
                          [p p p p p p p p]
                          [- - - - - - - -]
                          [- - - - - - - -]
                          [- - - b - - - -]
                          [- - - - - - - -]
                          [P P P P P - P P]
                          [R N B Q - K - R]]))




#_#_^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
    (clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer])

  ^{:nextjournal.clerk/visibility {:code :hide :result :show}}
  (t/view-test-case (t/expect-chess-moves {:piece :P
                                           :extra-checks [{} {} {:captures '(:p)}]}

                                          '[[- - - - -]
                                            [- - - - -]
                                            [- - p - -]
                                            [- P - - -]
                                            [- - - - -]]

                                          '[[- - - - -] [- - - - -] [- - - - -]
                                            [- - - - -] [- P - - -] [- - - - -]
                                            [- P p - -] [- - p - -] [- - P Q -]
                                            [- - - - -] [- - - - -] [- - - - -]
                                            [- - - - -] [- - - - -] [- - - - -]]))


^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer])
^{::clerk/visibility {:code :hide :result :show}
  ::clerk/viewers (concat [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer] clerk/default-viewers)}
(t/view-test-case

 (t/expect-chess-moves {:piece :K}
                    ;; K cannot move forward
                    '[[- - - - -]
                      [- - - r -]
                      [K - - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -] [- - - - -]
                      [- - - r -] [- - - r -] [- - - r -]
                      [- - - - -] [- - - - -] [- K - - -]
                      [K - - - -] [- K - - -] [- - - - -]
                      [- - - - -] [- - - - -] [- - - - -]]))
