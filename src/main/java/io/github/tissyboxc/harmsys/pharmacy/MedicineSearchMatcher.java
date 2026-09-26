package io.github.tissyboxc.harmsys.pharmacy;

import com.github.promeg.pinyinhelper.Pinyin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 药品搜索匹配器：同时支持中文原文、全拼、拼音首字母、编码/规格/厂家和近似拼写。
 */
public final class MedicineSearchMatcher {

  private MedicineSearchMatcher() {}

  /**
   * 计算匹配得分，得分越高排序越靠前；返回 0 表示不匹配。
   *
   * @param keyword 用户输入，可包含空格分隔的多个关键词
   * @param fields 药品可搜索字段，通常传入名称、编码、规格、厂家
   */
  public static int score(String keyword, String... fields) {
    List<String> terms = splitTerms(keyword);
    if (terms.isEmpty()) return 1;

    List<SearchField> searchFields = new ArrayList<>();
    for (String field : fields) {
      if (field == null || field.isBlank()) continue;
      searchFields.add(SearchField.of(field));
    }
    if (searchFields.isEmpty()) return 0;

    int total = 0;
    for (String term : terms) {
      int best = 0;
      for (SearchField field : searchFields) {
        best = Math.max(best, field.score(term));
      }
      if (best == 0) return 0;
      total += best;
    }
    return total;
  }

  private static List<String> splitTerms(String keyword) {
    if (keyword == null || keyword.isBlank()) return List.of();
    List<String> terms = new ArrayList<>();
    for (String term : keyword.trim().split("\\s+")) {
      if (!term.isBlank()) terms.add(term);
    }
    return terms;
  }

  private static String normalize(String value) {
    if (value == null) return "";
    return value.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-_/（）()]+", "");
  }

  private static int scoreText(String query, String candidate, int weight) {
    if (query.isBlank() || candidate.isBlank()) return 0;
    if (candidate.equals(query)) return 1000 * weight;
    if (candidate.startsWith(query)) return 800 * weight;
    if (candidate.contains(query)) return 600 * weight;
    return 0;
  }

  /** 药品单个字段的多种表示形式。 */
  private record SearchField(
      String original,
      String normalizedOriginal,
      String fullPinyin,
      String initials) {

    static SearchField of(String value) {
      String fullPinyin = Pinyin.toPinyin(value, "").toLowerCase(Locale.ROOT);
      String initials = buildInitials(value);
      return new SearchField(
          normalize(value),
          normalize(value),
          normalize(fullPinyin),
          normalize(initials));
    }

    int score(String rawTerm) {
      String term = normalize(rawTerm);
      if (term.isBlank()) return 0;

      String termPinyin = Pinyin.toPinyin(rawTerm, "").toLowerCase(Locale.ROOT);
      int best =
          Math.max(
              scoreText(term, normalizedOriginal, 5),
              Math.max(
                  scoreText(term, fullPinyin, 4),
                  Math.max(
                      scoreText(termPinyin, fullPinyin, 4),
                      scoreText(termPinyin, initials, 3))));

      // 输入本身是中文时，将其中文拼音与候选拼音整体比较，处理同音、近音字。
      if (best == 0 && containsChinese(rawTerm) && termPinyin.length() >= 3) {
        int commonLength = Math.min(termPinyin.length(), fullPinyin.length());
        int prefixDistance =
            levenshtein(
                termPinyin.substring(0, commonLength),
                fullPinyin.substring(0, commonLength));
        if (prefixDistance <= 1) best = 220;
      }

      // 只在足够长的纯字母输入上做近似匹配，避免短词产生大量误召回。
      if (best == 0 && term.length() >= 4 && term.chars().allMatch(Character::isLetter)) {
        int allowance = term.length() >= 6 ? 2 : 1;
        int fullDistance =
            levenshtein(term, fullPinyin.substring(0, Math.min(fullPinyin.length(), term.length())));
        int initialDistance =
            levenshtein(term, initials.substring(0, Math.min(initials.length(), term.length())));
        int distance = Math.min(fullDistance, initialDistance);
        if (distance <= allowance) best = 250;
      }
      return best;
    }

    private static boolean containsChinese(String value) {
      if (value == null) return false;
      for (int i = 0; i < value.length(); i++) {
        if (Pinyin.isChinese(value.charAt(i))) return true;
      }
      return false;
    }

    private static String buildInitials(String value) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (Pinyin.isChinese(c)) {
          String pinyin = Pinyin.toPinyin(c);
          if (!pinyin.isBlank()) builder.append(pinyin.charAt(0));
        } else if (Character.isLetterOrDigit(c)) {
          builder.append(Character.toLowerCase(c));
        }
      }
      return builder.toString();
    }

    private static int levenshtein(String left, String right) {
      int[] previous = new int[right.length() + 1];
      int[] current = new int[right.length() + 1];
      for (int j = 0; j <= right.length(); j++) previous[j] = j;
      for (int i = 1; i <= left.length(); i++) {
        current[0] = i;
        for (int j = 1; j <= right.length(); j++) {
          int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
          current[j] = Math.min(
              Math.min(current[j - 1] + 1, previous[j] + 1),
              previous[j - 1] + cost);
        }
        int[] swap = previous;
        previous = current;
        current = swap;
      }
      return previous[right.length()];
    }
  }
}
