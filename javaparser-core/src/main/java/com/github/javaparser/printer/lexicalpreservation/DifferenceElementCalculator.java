/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2020 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.concretesyntaxmodel.*;
import com.github.javaparser.printer.lexicalpreservation.LexicalDifferenceCalculator.CsmChild;

import java.util.*;

class DifferenceElementCalculator {
    static boolean matching(CsmElement a, CsmElement b) {
        if (a instanceof CsmChild) {
            if (b instanceof CsmChild) {
                CsmChild childA = (CsmChild) a;
                CsmChild childB = (CsmChild) b;
                return childA.getChild().equals(childB.getChild());
            } else if (b instanceof CsmToken) {
                return false;
            } else if (b instanceof CsmIndent) {
                return false;
            } else if (b instanceof CsmUnindent) {
                return false;
            } else {
                throw new UnsupportedOperationException(a.getClass().getSimpleName()+ " "+b.getClass().getSimpleName());
            }
        } else if (a instanceof CsmToken) {
            if (b instanceof CsmToken) {
                // fix #2382:
                // Tokens are described by their type AND their content
                // and TokenContentCalculator. By using .equals(), all
                // three values are compared.
                CsmToken childA = (CsmToken)a;
                CsmToken childB = (CsmToken)b;
                // TODO: Consider mixed line endings as equal? (equivalent?)
                return childA.equals(childB);
            } else if (b instanceof CsmChild) {
                return false;
            } else if (b instanceof CsmIndent) {
                return false;
            } else if (b instanceof CsmUnindent) {
                return false;
            } else {
                throw new UnsupportedOperationException(a.getClass().getSimpleName()+ " "+b.getClass().getSimpleName());
            }
        } else if (a instanceof CsmIndent) {
            return b instanceof CsmIndent;
        } else if (a instanceof CsmUnindent) {
            return b instanceof CsmUnindent;
        }
        throw new UnsupportedOperationException(a.getClass().getSimpleName()+ " "+b.getClass().getSimpleName());
    }

    private static boolean replacement(CsmElement a, CsmElement b) {
        if (a instanceof CsmIndent || b instanceof CsmIndent || a instanceof CsmUnindent || b instanceof CsmUnindent) {
            return false;
        }
        if (a instanceof CsmChild) {
            if (b instanceof CsmChild) {
                CsmChild childA = (CsmChild) a;
                CsmChild childB = (CsmChild) b;
                return childA.getChild().getClass().equals(childB.getChild().getClass());
            } else if (b instanceof CsmToken) {
                return false;
            } else {
                throw new UnsupportedOperationException(a.getClass().getSimpleName()+ " "+b.getClass().getSimpleName());
            }
        } else if (a instanceof CsmToken) {
            if (b instanceof CsmToken) {
                CsmToken childA = (CsmToken)a;
                CsmToken childB = (CsmToken)b;
                return childA.getTokenType() == childB.getTokenType();
            } else if (b instanceof CsmChild) {
                return false;
            }
        }
        throw new UnsupportedOperationException(a.getClass().getSimpleName()+ " "+b.getClass().getSimpleName());
    }

    /**
     * Find the positions of all the given children.
     */
    private static Map<Node, Integer> findChildrenPositions(LexicalDifferenceCalculator.CalculatedSyntaxModel calculatedSyntaxModel) {
        Map<Node, Integer> positions = new HashMap<>();
        for (int i=0;i<calculatedSyntaxModel.elements.size();i++) {
            CsmElement element = calculatedSyntaxModel.elements.get(i);
            if (element instanceof CsmChild) {
                positions.put(((CsmChild)element).getChild(), i);
            }
        }
        return positions;
    }

    /**
     * Calculate the Difference between two CalculatedSyntaxModel elements, determining which elements were kept,
     * which were added and which were removed.
     */
    static List<DifferenceElement> calculate(LexicalDifferenceCalculator.CalculatedSyntaxModel original, LexicalDifferenceCalculator.CalculatedSyntaxModel after) {
        // For performance reasons we use the positions of matching children
        // to guide the calculation of the difference
        //
        // Suppose we have:
        //   qwerty[A]uiop
        //   qwer[A]uiop
        //
        // with [A] being a child and lowercase letters being tokens
        //
        // We would calculate the Difference between "qwerty" and "qwer" then we know the A is kept, and then we
        // would calculate the difference between "uiop" and "uiop"

        Map<Node, Integer> childrenInOriginal = findChildrenPositions(original);
        Map<Node, Integer> childrenInAfter = findChildrenPositions(after);

        List<Node> commonChildren = new LinkedList<>(childrenInOriginal.keySet());
        commonChildren.retainAll(childrenInAfter.keySet());
        commonChildren.sort(Comparator.comparingInt(childrenInOriginal::get));

        List<DifferenceElement> elements = new LinkedList<>();

        int originalIndex = 0;
        int afterIndex = 0;
        int commonChildrenIndex = 0;
        while (commonChildrenIndex < commonChildren.size()) {
            Node child = commonChildren.get(commonChildrenIndex++);
            int posOfNextChildInOriginal = childrenInOriginal.get(child);
            int posOfNextChildInAfter    = childrenInAfter.get(child);

            // TODO: Better naming of this condition -- Define "elements to add" logic within this condition
            // TODO: Better determination of if changes have occurred (e.g. can we be sure re: adding and removing elements?)
            boolean changesHaveOccurred = originalIndex < posOfNextChildInOriginal || afterIndex < posOfNextChildInAfter;
            if (changesHaveOccurred) {
                List<DifferenceElement> differenceElements = calculateImpl(
                        original.sub(originalIndex, posOfNextChildInOriginal),
                        after.sub(afterIndex, posOfNextChildInAfter)
                );
                elements.addAll(differenceElements);
            }

            elements.add(new Kept(new CsmChild(child)));
            originalIndex = posOfNextChildInOriginal + 1;
            afterIndex = posOfNextChildInAfter + 1;
        }


        // TODO: Better naming of this condition -- Define "elements to add" logic within this condition
        // TODO: Better determination of if changes have occurred (e.g. can we be sure re: adding and removing elements?)
        boolean changesHaveOccurred = originalIndex < original.elements.size() || afterIndex < after.elements.size();
        if (changesHaveOccurred) {
            List<DifferenceElement> differenceElements = calculateImpl(
                    original.sub(originalIndex, original.elements.size()),
                    after.sub(afterIndex, after.elements.size())
            );
            // Add differences
            elements.addAll(differenceElements);
        }
        return elements;
    }

    private static void considerRemoval(NodeText nodeTextForChild, List<DifferenceElement> elements) {
        for (TextElement el : nodeTextForChild.getElements()) {
            if (el instanceof ChildTextElement) {
                ChildTextElement cte = (ChildTextElement) el;
                considerRemoval(LexicalPreservingPrinter.getOrCreateNodeText(cte.getChild()), elements);
            } else if (el instanceof TokenTextElement) {
                TokenTextElement tte = (TokenTextElement) el;
                elements.add(new Removed(new CsmToken(tte.getTokenKind(), tte.getText())));
            } else {
                throw new UnsupportedOperationException(el.toString());
            }
        }
    }

    private static int considerRemoval(CsmElement removedElement, int originalIndex, List<DifferenceElement> elements) {
        boolean dealtWith = false;
        if (removedElement instanceof CsmChild) {
            CsmChild removedChild = (CsmChild) removedElement;
            if (removedChild.getChild() instanceof Type && removedChild.getChild().getParentNode().isPresent() &&
                    removedChild.getChild().getParentNode().get() instanceof VariableDeclarator) {
                NodeText nodeTextForChild = LexicalPreservingPrinter.getOrCreateNodeText(removedChild.getChild());
                considerRemoval(nodeTextForChild, elements);
                originalIndex++;
                dealtWith = true;
            }
        }
        if (!dealtWith) {
            elements.add(new Removed(removedElement));
            originalIndex++;
        }
        return originalIndex;
    }

    // TODO: Rename me.
    private static List<DifferenceElement> calculateImpl(LexicalDifferenceCalculator.CalculatedSyntaxModel original,
                                                         LexicalDifferenceCalculator.CalculatedSyntaxModel after) {
        List<DifferenceElement> elements = new LinkedList<>();

        int originalIndex = 0;
        int afterIndex = 0;

        // We move through the two CalculatedSyntaxModel, moving both forward when we have a match
        // and moving just one side forward when we have an element kept or removed

        boolean originalElementsRemaining = originalIndex < original.elements.size();
        boolean afterElementsRemaining = afterIndex < after.elements.size();
        do {
            // TODO: These "exhausted" variables can be defined as the inverse of elements remaining.
            boolean exhaustedAfterElements = afterIndex >= after.elements.size();
            boolean exhaustedOriginalElements = originalIndex >= original.elements.size();

            if (originalElementsRemaining && exhaustedAfterElements) {
                // Elements are present in the original, but not after.
                // Thus can shortcut as all remaining elements must have been removed.
                CsmElement removedElement = original.elements.get(originalIndex);
                originalIndex = considerRemoval(removedElement, originalIndex, elements);
            } else if (exhaustedOriginalElements && afterElementsRemaining) {
                // No original elements remain but there are still more elements after.
                // Thus can shortcut as all remaining elements must have been added.
                elements.add(new Added(after.elements.get(afterIndex)));
                afterIndex++;
            } else {
                // Still elements remaining before/after that must be compared.
                // Thus, must look more closely to determine if they have been added/removed.
                CsmElement nextOriginal = original.elements.get(originalIndex);
                CsmElement nextAfter = after.elements.get(afterIndex);

                if ((nextOriginal instanceof CsmMix) && (nextAfter instanceof CsmMix)) {
                    if (((CsmMix) nextAfter).getElements().equals(((CsmMix) nextOriginal).getElements())) {
                        // They are equal -- we are just going to keep everything as it is.
                        // Thus we can shortcut as there is no reason to deal with a Reshuffled.
                        ((CsmMix) nextAfter).getElements().forEach(el -> elements.add(new Kept(el)));
                    } else {
                        elements.add(new Reshuffled((CsmMix) nextOriginal, (CsmMix) nextAfter));
                    }
                    originalIndex++;
                    afterIndex++;
                } else if (matching(nextOriginal, nextAfter)) {
                    elements.add(new Kept(nextOriginal));
                    originalIndex++;
                    afterIndex++;
                } else if (replacement(nextOriginal, nextAfter)) {
                    originalIndex = considerRemoval(nextOriginal, originalIndex, elements);
                    elements.add(new Added(nextAfter));
                    afterIndex++;
                } else {
                    // We can try to remove the element or add it and look which one leads to the least difference (similar to Levenshtein distance between strings)
                    List<DifferenceElement> addingElements = calculate(original.from(originalIndex), after.from(afterIndex + 1));
                    List<DifferenceElement> removingElements = null;
                    if (costOfDifferences(addingElements) > 0) {
                        removingElements = calculate(original.from(originalIndex + 1), after.from(afterIndex));
                    }

                    if (removingElements == null || costOfDifferences(removingElements) > costOfDifferences(addingElements)) {
                        elements.add(new Added(nextAfter));
                        afterIndex++;
                    } else {
                        elements.add(new Removed(nextOriginal));
                        originalIndex++;
                    }
                }
            }

            // Variables to indicate if we have reached the end of either the originals or the afters.
            // Note: Deliberately updated at the end, so that the while() condition uses up-to-date values.
            originalElementsRemaining = originalIndex < original.elements.size();
            afterElementsRemaining = afterIndex < after.elements.size();

        } while (originalElementsRemaining || afterElementsRemaining);

        return elements;
    }

    /**
     * @return The number of changes (additions/removals) as the cost.
     */
    private static long costOfDifferences(List<DifferenceElement> elements) {
        return elements.stream().filter(e -> !(e instanceof Kept)).count();
    }


    /**
     * Remove from the difference all the elements related to indentation.
     * This is mainly intended for test purposes.
     */
    static void removeIndentationElements(List<DifferenceElement> elements) {
        elements.removeIf(el -> el.getElement() instanceof CsmIndent || el.getElement() instanceof CsmUnindent);
    }
}
