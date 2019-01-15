/**
 * An element that holds the similarity between two CustomElements. It is only created when the similarity
 * is of such height that the CustomElements are possible matches.
 */
class Partner {
    private CustomElement partner;
    private Double similarity;

    Partner(CustomElement partner, Double similarity) {
        this.partner = partner;
        this.similarity = similarity;
    }

    Double getSimilarity() {
        return this.similarity;
    }

    CustomElement getPartner() {
        return this.partner;
    }
}
