import spacy
nlp = spacy.load("en_core_web_sm")
# bash : uv run python -m spacy download en_core_web_sm
def _get_lemma(text)->list[str]:
    doc = nlp(text)
    lemmas = []
    ignore_words = {
        "a", "I", "you", "yours", "he", "his", "she", "her", "my", "mine", "we", "ours",
        "it", "so", "OK", "ok", "because", "an", "the", "and", "or", "but", "if", "then",
        "else", "for", "to", "of", "in", "on", "at", "by", "with", "as", "from", "are",
        "is", "were", "be", "s",
        '"', "(", ")", "[", "]", "{", "}", "<", ">", ".", ",", "!", "?", ";", ":",
        "'", "\\", "-", "_", "/", "|", "+", "=", "*", "&", "^", "%", "$", "#", "@",
        "~", "`", "laugh", "laughs", "laughter", "applause", "applauds", "applauding",
        "claps", "clapping", "cheers", "cheering"
    }
    for token in doc:
        if token.lemma_ not in ignore_words and not token.lemma_.isdigit():
            lemmas.append(token.lemma_)
    return lemmas

if __name__ == "__main__":
    text = "I am running and he is Running too."
    lemmas = _get_lemma(text)
    print(lemmas)  # Output: ['run', 'run']