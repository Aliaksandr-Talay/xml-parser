package by.me.service;

import by.me.data.Node;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeBuilder {

    private final Pattern anyOpeningTagPattern =
            Pattern.compile("(<)[A-z\\-0-9\\$\\.]*(:)?[A-z\\-0-9\\s\\$\\.\\=\\\"]*(>)");
    private final Pattern tagNamePattern = Pattern.compile("(<)[A-z\\-0-9\\.]*(:)?[A-z\\-0-9]*");

    private Map<Integer, Integer> tagsStartMap;


    private void mapTags(String string) {
        Matcher anyOpeningTagMatcher = anyOpeningTagPattern.matcher(string);

        tagsStartMap = new HashMap<>();

        while (anyOpeningTagMatcher.find()) {
            String name = parseName(string, anyOpeningTagMatcher.start());
            Map<Integer, Integer> openingClosingTagsStartIndexesMap = new HashMap<>();

            Pattern openingTagPattern =
                    Pattern.compile("(<" + name + ")[A-z\\-0-9\\$\\.]*(:)?[A-z\\-0-9\\s\\$\\.\\=\"]*(>)");
            Pattern endTagPattern = Pattern.compile("</" + name + ">");
            Matcher openingTagMatcher = openingTagPattern.matcher(string);
            Matcher closingTagMatcher = endTagPattern.matcher(string);
            List<Integer> openingTagsStartIndexes = new ArrayList<>();
            List<Integer> closingTagsStartIndexes = new ArrayList<>();
            while (openingTagMatcher.find()) {
                openingTagsStartIndexes.add(openingTagMatcher.start());
            }
            while (closingTagMatcher.find()) {
                closingTagsStartIndexes.add(closingTagMatcher.start());
            }
            openingClosingTagsStartIndexesMap = createMapForTag(openingTagsStartIndexes, closingTagsStartIndexes);
            for (int openingTagStartIndex : openingClosingTagsStartIndexesMap.keySet()) {
                tagsStartMap.put(openingTagStartIndex, openingClosingTagsStartIndexesMap.get(openingTagStartIndex));
            }
        }
    }


    private Map<Integer, Integer> createMapForTag(List<Integer> openingTagsStartIndexes, List<Integer> closingTagsStartIndexes) {
        Map<Integer, Integer> resultMap = new HashMap<>();
        Collections.sort(openingTagsStartIndexes);
        Collections.sort(closingTagsStartIndexes);
        int closingMaxIndex = closingTagsStartIndexes.stream().max(Comparator.naturalOrder()).get();
        for (Integer closingTagsStartIndex : closingTagsStartIndexes) {
            boolean valueNotDefined = true;
            for (int i = openingTagsStartIndexes.size() - 1; valueNotDefined && i >= 0; i--) {
                int openingIndex = openingTagsStartIndexes.get(i);
                int closingIndex = closingTagsStartIndex;
                if (closingIndex > openingIndex) {
                    resultMap.put(openingIndex, closingIndex);
                    openingTagsStartIndexes.set(i, closingMaxIndex);
                    valueNotDefined = false;
                }
            }
        }
        return resultMap;
    }


    public Node build(String inputString, int startAt) throws Exception {
        inputString = inputString.trim();
        Node outputNode = new Node();
        List<Node> childNodes = new ArrayList<>();
        Matcher anyOpeningTagMatcher = anyOpeningTagPattern.matcher(inputString);
        int startOfOpeningTag;
        int endOfOpeningTag;
        String name = "";

        if (anyOpeningTagMatcher.find(startAt)) {
            startOfOpeningTag = anyOpeningTagMatcher.start();
            endOfOpeningTag = anyOpeningTagMatcher.end();
            name = parseName(inputString, anyOpeningTagMatcher.start());

            if (!"".equals(name)) {
                outputNode.setName(name);
            } else {
                throw new Exception("Can not parse name of tag at position" + startOfOpeningTag);
            }

            if (inputString.substring(startOfOpeningTag, endOfOpeningTag).contains("=")) {
                Map<String, String> attributes;
                attributes = parseAttributes(inputString, startOfOpeningTag, endOfOpeningTag);
                outputNode.setAttributes(attributes);
            }

            if (tagsStartMap == null) {
                mapTags(inputString);
            }

            Set<Integer> innerTagsStartIndexes = getIndexesOfInnerTags(inputString, startOfOpeningTag);

            if (!innerTagsStartIndexes.isEmpty()) {
                for (int index : innerTagsStartIndexes) {
                    Node node = build(inputString, index);
                    childNodes.add(node);
                }
            } else {
                int closingTagStartIndex = tagsStartMap.get(startOfOpeningTag);
                String content = inputString.substring(endOfOpeningTag, closingTagStartIndex).trim();
                if (!"".equals(content)) {
                    outputNode.setContent(content);
                }
            }

            if (!childNodes.isEmpty()) {
                outputNode.setChildNodes(childNodes);
            }
        }
        return outputNode;
    }


    public String parseName(String input, int startPosition) {

        Matcher nameMatcher = tagNamePattern.matcher(input);
        if (nameMatcher.find(startPosition)) {
            return input.substring(nameMatcher.start() + 1, nameMatcher.end());
        }
        return "";
    }


    public Map<String, String> parseAttributes(String inputString, int startAt, int finishAt) {
        String tag = inputString.substring(startAt, finishAt);
        Map<String, String> resultMap = new HashMap<>();
        Pattern attributePattern = Pattern.compile("[A-z\\-0-9\\$\\.]*(=)('|\")[\\(\\)A-z\\-0-9\"\\s]*('|\")");
        Matcher matcher = attributePattern.matcher(tag);
        while (matcher.find()) {
            String attribute = matcher.group();
            int equalsIndex = attribute.indexOf("=");

            String attributeKey = attribute.substring(0, equalsIndex);
            String attributeValue = attribute.substring(equalsIndex + 2, attribute.length() - 1);
            resultMap.put(attributeKey, attributeValue);
        }
        return resultMap;
    }


    public Set<Integer> getIndexesOfInnerTags(String inputString, int tagStartIndex) {
        if (tagsStartMap == null) {
            mapTags(inputString);
        }
        int closingTagStartIndex = tagsStartMap.get(tagStartIndex);
        Set<Integer> outputSet = new HashSet<>();
        for (int index : tagsStartMap.keySet()) {
            if (index > tagStartIndex && index < closingTagStartIndex) {
                outputSet.add(index);
            }
        }
        Set<Integer> deepInternalTagSet = new HashSet<>();
        for (int index : outputSet) {
            int closingIndex = tagsStartMap.get(index);

            for (int index2 : outputSet) {
                int closingIndex2 = tagsStartMap.get(index2);

                if (index < index2 && closingIndex > closingIndex2) {
                    deepInternalTagSet.add(index2);
                }
            }
        }
        for (int index : deepInternalTagSet) {
            outputSet.remove(index);
        }
        return outputSet;
    }


}
