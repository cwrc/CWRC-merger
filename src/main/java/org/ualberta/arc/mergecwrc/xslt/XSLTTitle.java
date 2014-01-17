package org.ualberta.arc.mergecwrc.xslt;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.ualberta.arc.mergecwrc.CWRCException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mpm1
 */
public class XSLTTitle {

    // Test
    /*public static void main(String args[]) {
    XSLTTitle tester = new XSLTTitle();
    
    String testString = "La petite poule d’eau (rev. 1970) / Where Nests The Water Hen (trans. 1951)";
    System.out.println("Test Title:");
    System.out.println(testString);
    
    try {
    NodeList titles = tester.extractTitles(testString);
    
    for (int i = 0; i < titles.getLength(); ++i) {
    System.out.println();
    
    Element element = (Element) titles.item(i);
    
    StringWriter sw = new StringWriter();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.setOutputProperty(OutputKeys.INDENT, "yes");
    t.transform(new DOMSource(element), new StreamResult(sw));
    
    System.out.println(sw.toString());
    }
    } catch (Exception ex) {
    System.err.println("Error: " + ex.getMessage());
    }
    }*/
    private static enum CEWWType {

        BOOK("Books"),
        PEIODI("Periodicals"),
        UNVERI("Unverified titles");
        private String text;

        CEWWType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public boolean isEqual(String value) {
            return StringUtils.equals(text, value);
        }
    }
    private static final List<String> roles = new ArrayList<String>();
    private static final String[] excludeTitles = new String[]{
        "WSW (West South West)",
        "White Lies (For My Mother)",
        "La querelle du régionalisme au Québec (1904-1931): Vers l’autonomisation de la literature québécoise",
        "The History Show (Missionaries, Muskeg and Mounties)",
        "Le Centre interuniversitaire d'analyse du discours et de sociocritique des textes (CIADEST)",
        "(Alive)",
        "Sortie d'elle(s) mutante",
        "Histoire sombre (mise en lumière)",
        "(Parenthèses)",
        "Une collection de lumières (Poèmes choisis 1984-2004)",
        "Monarch (Lay Your Jewelled Head Down)",
        "Dont Go (Girls and Boys)",
        "Little Giant (Miss-top-ashish): the story of Henry Kelsey",
        "Emphysema (A Love Story)",
        "The Red Priest (Eight Ways to Say Goodbye)",
        "Grandir (En hommage à ma fille)",
        "Two Miniatures ('Little March')",
        "Choral Symphony 'This Land' (Symphony No. 2)",
        "Duo Sonata for Violin and Piano (Sonata No. 1)",
        "Sonata II (A Correspondence)",
        "Barcarole for Flute (or Piccolo) and Piano",
        "(W)right State of Mind",
        "(out)(in)... the OPEN",
        "ANSWER! (question was irrelevant)",
        "(...question was ignored...)",
        "Technologie Salvatrice II : Les polytechniciens(nes)",
        "War Surgery (1981-1995): A Jazz Catafalque",
        "Gwethalyn Graham (1913-65): A Liberated Woman in a Conventional Age",
        "I'm Alice (Beauty Queen?)",
        "2. Saying Goodbye (A Promise Broken)",
        "(8)ight",
        "Spooks (A Haunting Comedy)",
        "I Travelled (Unreleased)",
        "Katherine Wallis, Canadian Sculptor (1861-1954)",
        "Margaret Fuller (1810-1850)",
        "Notes on Browning's Works (Coles Notes, 550)",
        "Writing Maniac: How I Grew Up To Be A Writer (And You Can, Too!)",
        "The Drowning of Wasyl Nemitchuk (A Fine Coloured Easter Egg)",
        "TRANS(per)FORMING Nina Arsenault: An Unreasonable Body of Work",
        "Serpent (w)rite",
        "Concerto for Double Bass (or Cello) and Orchestra",
        "Saxophone Quartet (Going Downstairs)",
        "Muscular Dystrophy (Genetic and Developmental Diseases and Disorders)",
        "The Audit (Line Item: Foster Child)",
        "Breakdown of System (and malfunction in 5 different ways)",
        "poem(s) on the stairs",
        "Goodnight Desdemona (Good Morning Juliet)",
        "Ten Things We Did (And Probably Shouldn't Have)",
        "La baie du Nord (ou, Menfou Carcajou)",
        "Gaspard (ou, Le chemin des montagnes)",
        "Passerelle (collected poems 1964-2006)",
        "Le Survenant (1945) and Marie-Didace (1947)",
        "Le monde de Francis et Nathalie (16 titles)",
        "Chiens divers (et autres faits écrasés)",
        "Nicolas, le fils du Nil (a novel in poetry)",
        "Nation Builders (Barnardo Children in Canada)",
        "Canada Mosaic (3 folk songs)",
        "Music of our Time / Musique de notre temps (9 volumes)",
        "Renovated Rhymes (John V. Hicks)",
        "Tamarack's 25th Anniversary (Composition in Three Movements)",
        "Madame Benoît’s Library of Canadian Cooking Volume 2: Beef (cont.) Lamb, Veal, Pork, Ham",
        "Madame Benoît’s Library of Canadian Cooking Volume 4: Eggs (cont.), Green Vegetables",
        "Madame Benoît’s Library of Canadian Cooking Volume 5: Green Vegetables (cont.) Dried Vegetables, Fish and Shellfish, Electric Blenders, Hot and Cold Sauces, Herbs and Food Seasonings",
        "Madame Benoît’s Library of Canadian Cooking Volume 10: Ice Cream (cont.), Candies, Pressure Cooking, Jams and Jellies, Health Foods, Freezing",
        "Couche Avec Moi (c'est l'hiver)",
        "Pourquoi Je Suis Separatiste: Suivi De Quatres Autres ecrits Politiques (Marcel Chaput and Michel Venne)",
        "Des nouvelles de Martha (roman épistolaire)",
        "Midaregami (tangled hair)",
        "A Tax on Pochsy, or, The Audit (Line Item: Foster Child)",};
    private static final String[] notes = new String[]{
        "4-CD box set",
        "Adap.",
        "adap.",
        "Adapt.",
        "after",
        "also",
        "arrangement",
        "arr.",
        "art book",
        "as",
        "udio-book",
        "ballet",
        "bilingual",
        "Braille version",
        "broadside",
        "cantata",
        "cassette",
        "CD-ROM",
        "Chapbook",
        "chapbook",
        "Christmas cantata",
        "Collab.",
        "collection",
        "concert",
        "contributor",
        "cookbook",
        "copyscript",
        "dance",
        "direct-to-video",
        "documentary",
        "Documentary",
        "drama",
        "DVD",
        "Edited",
        "Ed.",
        "Editor",
        "editor",
        "electronic resource",
        "electronic tape",
        "electronic version",
        "EP",
        "essays",
        "et al.",
        "EU",
        "fiction",
        "film",
        "final",
        "flute",
        "for",
        "French",
        "full-length ballet",
        "guidebook",
        "haiku",
        "hand-bound limited edition",
        "illustrated by",
        "in",
        "includes",
        "interviews",
        "limited edition",
        "liturgical",
        "Live",
        "live",
        "live recording",
        "live recordings",
        "LP",
        "manga",
        "monologue",
        "multimedia",
        "musical",
        "music-drama",
        "nonfiction",
        "omnibus",
        "on",
        "one-act play",
        "opera",
        "original",
        "overture",
        "philosphy",
        "photographs",
        "poetic drama",
        "poetry",
        "Posthumous",
        "print",
        "Prod.",
        "prod.",
        "Psalm",
        "puppet drama",
        "radio",
        "received",
        "recipes",
        "recording",
        "re-edited",
        "Reissued",
        "released",
        "re-release",
        "remastered",
        "retelling",
        "rev.",
        "Rev.",
        "revised",
        "romance",
        "score",
        "script",
        "self-illustrated",
        "short",
        "song",
        "soprano",
        "sound recording",
        "spoken",
        "substatial",
        "subtitle",
        "teen fiction",
        "television",
        "TV",
        "textbook",
        "text by",
        "text",
        "Trans.",
        "trans.",
        "various",
        "version",
        "vol",
        "webcomic",
        "Webcomic",
        "with",
        "woodwind",
        "workbook",
        "works",
        "writing manual"
    };
    private static final String[] series = new String[]{
        "book",
        "series",
        "vol.",
        "volume",
        "trilogy",
        "tetralogy",
        "duology",
        "sextet"
    };

    static {
        roles.add("eds");
    }
    private final List<String> titleList = new ArrayList<String>();

    public String addToTitleList(String input) {
        if (!titleList.contains(input)) {
            titleList.add(input);
        }

        return input;
    }

    public NodeList getTitleList() throws Exception {
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        return new NodeList() {

            public Node item(int i) {
                Element element = doc.createElement("title");

                element.setTextContent(titleList.get(i));

                return element;
            }

            public int getLength() {
                return titleList.size();
            }
        };
    }

    public String getPlace(String input) {
        int index = input.indexOf(":");

        if (index > -1) {
            return input.substring(0, index);
        }

        return "";
    }

    // Functions for reading CEWW datra lines.
    public String readCEWWBracket(Title title, Reader reader, String type) throws IOException {
        int val = 0;
        StringBuilder builder = new StringBuilder();

        while ((val = reader.read()) > -1) {
            switch (val) {
                case ')':
                    String data = builder.toString().trim();
                    if (CEWWType.BOOK.isEqual(type) || CEWWType.UNVERI.isEqual(type)) {
                        title.addDate(data);
                        return null;
                    } else if (CEWWType.PEIODI.isEqual(type)) {
                        if (data.toLowerCase().startsWith("also as various titles, including")) {
                            String titles[] = data.substring(33).trim().split("and");
                            for (String alternate : titles) {
                                title.addOtherTitle(WordUtils.capitalizeFully(alternate.trim()));
                            }
                        } else {
                            title.addPlace(WordUtils.capitalizeFully(data));
                        }
                        return null;
                    }
                    return builder.toString().trim();

                default:
                    builder.append((char) val);
            }
        }

        return builder.toString();
    }

    public String readCEWWSquareBracket(Title title, Reader reader) throws IOException {
        int val = 0;
        StringBuilder builder = new StringBuilder();

        while ((val = reader.read()) > -1) {
            switch (val) {
                case ']':
                    String data = builder.toString().trim();
                    if (data.toLowerCase().startsWith("also titled")) {
                        title.addOtherTitle(data.substring(11).trim());
                    }
                    return null;

                default:
                    builder.append((char) val);
            }
        }

        return builder.toString();
    }

    private static void addAlternateTitle(Title lastTitle, Title title, int index) {
        lastTitle.addOtherTitle(title.title.substring(index).trim());

        for (String date : title.getDates()) {
            lastTitle.addDate(date);
        }

        for (String note : title.getNotes()) {
            lastTitle.addNote(note);
        }

        for (String places : title.getPlaces()) {
            lastTitle.addPlace(places);
        }
    }

    public static boolean addCEWWOriginInfo(NodeList list1, NodeList list2) {
        if (list1 == null || list1.getLength() < 1) {
            return false;
        }

        if (list2 == null || list2.getLength() < 1) {
            return false;
        }

        return true;
    }

    public NodeList readCEWWTitleEntries(String input, String type) throws IOException, CWRCException {
        List<Title> output = new ArrayList<Title>();

        if (!StringUtils.isBlank(input)) {
            Reader reader = new StringReader(input);
            int val = 0;
            StringBuilder builder = new StringBuilder();
            Title lastTitle = null;
            Title title = new Title();

            // Read the input.
            String bracketData;
            while ((val = reader.read()) > -1) {
                switch (val) {
                    case '[':
                        bracketData = readCEWWSquareBracket(title, reader);
                        if (bracketData != null) {
                            builder.append('[');
                            builder.append(bracketData);
                            builder.append(']');
                        }
                        break;

                    case '(':
                        bracketData = readCEWWBracket(title, reader, type);
                        if (bracketData != null) {
                            builder.append('(');
                            builder.append(bracketData);
                            builder.append(')');
                        }
                        break;

                    case ';':
                        title.title = WordUtils.capitalizeFully(builder.toString().trim());
                        if (title.title.toLowerCase().startsWith("also titled")) {
                            addAlternateTitle(lastTitle, title, 11);
                        } else {
                            output.add(title);
                            lastTitle = title;
                        }

                        title = new Title();
                        builder.setLength(0);
                        break;

                    default:
                        builder.append((char) val);
                }
            }

            title.title = WordUtils.capitalizeFully(builder.toString().trim());
            if (title.title.toLowerCase().startsWith("also titled")) {
                addAlternateTitle(lastTitle, title, 11);
            } else {
                output.add(title);
            }
        }

        return new TitleNodes(output);
    }

    // Functions for reading CanWWr files
    public NodeList extractCriticNames(String input) throws CWRCException {
        //String[] nameStrings = input.split(" and ");
        String[] nameStrings = {input};
        List<Name> names = new ArrayList<Name>();

        for (String nameString : nameStrings) {
            Name name = new Name();
            name.setName(nameString);

            // Check if role is contained in the name
            for (String role : roles) {
                if (nameString.endsWith(", " + role)) {
                    int index = nameString.lastIndexOf(",");
                    name.setName(nameString.substring(0, index));
                    name.role = role;
                    break;
                }
            }

            names.add(name);
        }

        return new NameNodes(names);
    }

    public static String removeItalic(String input) {
        return input.replaceAll("(\\&lt;/?i\\&gt;)|(</?i>)", "");
    }

    public NodeList groupByGenre(NodeList nodeList) throws ParserConfigurationException {
        final List<String> output = new ArrayList<String>();

        for (int i = 0; i < nodeList.getLength(); ++i) {
            String value = nodeList.item(i).getTextContent();

            if (!output.contains(value)) {
                output.add(value);
            }
        }

        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        return new NodeList() {

            public Node item(int i) {
                Element element = doc.createElement("genre");
                element.setTextContent(output.get(i));
                return element;
            }

            public int getLength() {
                return output.size();
            }
        };
    }

    /**
     * Extracts all titles from a given input.
     * @param input The titles to be extracted.
     * @return All titles from the given input with the primary title on top.
     */
    public NodeList extractTitles(String input) throws IOException, CWRCException {
        List<Title> output = new ArrayList<Title>();

        Reader reader = new StringReader(input);
        int val = 0;
        StringBuilder builder = new StringBuilder();
        Title title = new Title();

        // First test the title against the excluded names
        for (String testTitle : excludeTitles) {
            if (StringUtils.equals(testTitle, input)) {
                title.title = input;
                output.add(title);
                return new TitleNodes(output);
            }
        }

        // Read the input.
        String bracketData;
        while ((val = reader.read()) > -1) {
            switch (val) {
                case '/':
                    title.title = builder.toString().trim();
                    output.add(title);
                    title = new Title();
                    builder.setLength(0);
                    break;

                case '(':
                    bracketData = inBracket(reader, title);
                    if (bracketData != null) {
                        builder.append("(");
                        builder.append(bracketData);
                        builder.append(")");
                    }
                    break;

                case '[':
                    output.add(readSquareBracket(reader));
                    break;

                default:
                    builder.append((char) val);
            }
        }

        title.title = builder.toString().trim();
        output.add(title);
        return new TitleNodes(output);
    }

    private static Title readSquareBracket(Reader reader) throws IOException {
        int val = 0;
        StringBuilder builder = new StringBuilder();
        Title title = new Title();

        while ((val = reader.read()) > -1) {
            switch (val) {
                case '(':
                    String bracketData = inBracket(reader, title);
                    if (bracketData != null) {
                        title.notes.add(bracketData);
                    }
                    break;

                case ']':
                    String outString = removeItalic(builder.toString());
                    if (outString.startsWith("ie:")) {
                        outString = outString.substring(3);
                    }
                    title.title = outString.trim();
                    return title;

                default:
                    builder.append((char) val);
            }
        }

        return title;
    }

    private static String inBracket(Reader reader, Title title) throws IOException {
        int val = 0;
        StringBuilder builder = new StringBuilder();

        while ((val = reader.read()) > -1) {
            switch (val) {
                case '(':
                    inBracket(reader, title);
                    break;

                case ')':
                    // check against list
                    String result = builder.toString();
                    result = removeItalic(result);

                    try {
                        Integer.parseInt(result);
                        return result;
                    } catch (NumberFormatException ex) {
                    }

                    for (String seriesVal : series) {
                        if (StringUtils.containsIgnoreCase(result, seriesVal)) {
                            title.setSeries(true);
                        }
                    }

                    /*for (String note : notes) {
                    if (result.startsWith(note)) {
                    title.addNote(result);
                    return null;
                    }
                    }*/
                    title.addNote(result);
                    return null;


                default:
                    builder.append((char) val);
            }
        }

        //TODO: Handle the information inside the bracket
        return builder.toString();


    }

    /**
     * A object used to represent the title given the information.
     */
    public static class Title {

        private String title;
        private boolean series = false;
        private List<String> notes = new ArrayList<String>();
        private List<String> dates = new ArrayList<String>();
        private List<String> places = new ArrayList<String>();
        private List<String> otherTitles = new ArrayList<String>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setSeries(boolean series) {
            this.series = series;
        }

        public boolean isSeries() {
            return series;
        }

        public List<String> getNotes() {
            return notes;
        }

        public void addNote(String note) {
            notes.add(note);
        }

        public List<String> getDates() {
            return dates;
        }

        public void addDate(String date) {
            dates.add(date);
        }

        public List<String> getPlaces() {
            return places;
        }

        public void addPlace(String place) {
            places.add(place);
        }

        public List<String> getOtherTitles() {
            return otherTitles;
        }

        public void addOtherTitle(String title) {
            otherTitles.add(title);
        }
    }

    private static class TitleNodes implements NodeList {

        private static Pattern checkGenre;

        {
            checkGenre = Pattern.compile("(\".*\"$)|(\".*\".*\\(.*\\)$)|('.*'$)|('.*'.*\\(.*\\)$)");
        }
        private List<Element> nodes;

        private static String extractQuotations(String input) {
            String checkVal = input.trim();

            if ((checkVal.startsWith("'") && checkVal.endsWith("'"))
                    || (checkVal.startsWith("\"") && checkVal.endsWith("\""))) {
                checkVal = checkVal.substring(1, checkVal.length() - 1);
            }

            return checkVal;
        }

        public TitleNodes(List<Title> titles) throws CWRCException {
            nodes = new ArrayList<Element>(titles.size());

            Document doc = null;
            Element main = null;

            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                main = doc.createElement("main");
                doc.appendChild(main);
            } catch (ParserConfigurationException ex) {
                throw new CWRCException(ex);
            }

            // Add each element to the nodelist
            boolean isAlternative = false;
            for (Title title : titles) {
                Element titleElement = doc.createElement("title");
                titleElement.setAttribute("isAlternative", Boolean.toString(isAlternative));
                titleElement.setAttribute("moreThanOne", Boolean.toString(titles.size() > 1));
                isAlternative = true;

                Element element = doc.createElement("title");
                element.setTextContent(extractQuotations(title.title));
                titleElement.appendChild(element);

                // Check the genre level
                element = doc.createElement("genre");
                if (title.isSeries() || title.getTitle().toLowerCase().endsWith("series")) {
                    element.setTextContent("s");
                } else if (checkGenre.matcher(title.getTitle()).matches()) {
                    element.setTextContent("a");
                } else {
                    element.setTextContent("m");
                }
                titleElement.appendChild(element);

                // Check for notes
                if (title.getNotes().size() > 0) {
                    Element notes = doc.createElement("notes");

                    for (String note : title.getNotes()) {
                        element = doc.createElement("note");
                        element.setTextContent(note);
                        notes.appendChild(element);
                    }

                    titleElement.appendChild(notes);
                }

                // Check for dates
                if (title.getDates().size() > 0) {
                    Element dates = doc.createElement("dates");

                    for (String date : title.getDates()) {
                        element = doc.createElement("date");
                        element.setTextContent(date);
                        dates.appendChild(element);
                    }

                    titleElement.appendChild(dates);
                }

                // Check for places
                if (title.getPlaces().size() > 0) {
                    Element places = doc.createElement("places");

                    for (String place : title.getPlaces()) {
                        element = doc.createElement("place");
                        element.setTextContent(place);
                        places.appendChild(element);
                    }

                    titleElement.appendChild(places);
                }

                // Check for otherTitles
                if (title.getOtherTitles().size() > 0) {
                    Element otherTitles = doc.createElement("titles");

                    for (String otherTitle : title.getOtherTitles()) {
                        element = doc.createElement("title");
                        element.setTextContent(otherTitle);
                        otherTitles.appendChild(element);
                    }

                    titleElement.appendChild(otherTitles);
                }

                main.appendChild(titleElement);
                nodes.add(titleElement);
            }
        }

        public Node item(int i) {
            return nodes.get(i);
        }

        public int getLength() {
            return nodes.size();
        }
    }

    public static class Name {

        private String name;
        private String role;

        public String getName() {
            return name;
        }

        public void setName(String title) {
            this.name = title;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    private static class NameNodes implements NodeList {

        private List<Element> nodes;

        public NameNodes(List<Name> names) throws CWRCException {
            nodes = new ArrayList<Element>(names.size());

            Document doc = null;
            Element main = null;

            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                main = doc.createElement("main");
                doc.appendChild(main);
            } catch (ParserConfigurationException ex) {
                throw new CWRCException(ex);
            }

            // Add each element to the nodelist
            for (Name name : names) {
                Element nameElement = doc.createElement("name");

                Element element = doc.createElement("namePart");
                element.setTextContent(name.getName());
                nameElement.appendChild(element);

                // Check the role
                if (name.getRole() != null) {
                    Element role = doc.createElement("role");

                    element = doc.createElement("roleTerm");
                    element.setTextContent(name.getRole());
                    role.appendChild(element);

                    nameElement.appendChild(role);
                }

                main.appendChild(nameElement);
                nodes.add(nameElement);
            }
        }

        public Node item(int i) {
            return nodes.get(i);
        }

        public int getLength() {
            return nodes.size();
        }
    }
}
