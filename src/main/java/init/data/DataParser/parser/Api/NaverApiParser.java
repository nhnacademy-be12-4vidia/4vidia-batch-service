package init.data.DataParser.parser.Api;

import com.fasterxml.jackson.databind.ObjectMapper;
import init.data.DataParser.DTO.Naver.NaverApiResponseDto;
import init.data.DataParser.DTO.Naver.NaverParsingDto;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.Publisher;
import init.data.DataParser.parser.common.Formatter;
import init.data.DataParser.repository.BookRepository;
import init.data.DataParser.repository.PublisherRepository;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class NaverApiParser {

  @PostConstruct
  void init() {
    searchAndSaveBooks("만화");
  }


  private final BookRepository bookRepository;
  private final PublisherRepository publisherRepository;

  @Value("${naver.api.client-id}")
  private String clientId;

  @Value("${naver.api.client-secret}")
  private String clientSecret;

  public void searchAndSaveBooks(String keyword) {

    final int display = 100;
    final int totalPages = 5;

    try {
      log.info("------{} 관련 도서 검색 시작------", keyword);
      log.info(clientSecret);

      String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

      for (int i = 0; i < totalPages; i++) {
        int start = 1 + (i * display);
        String apiUrl = "https://openapi.naver.com/v1/search/book.json?query=%s&display=%s&start=%s".formatted(encodedKeyword, display, start);

        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("X-Naver-Client-Id", clientId);
        con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String line;
        StringBuffer responseJson = new StringBuffer();
        while ((line = br.readLine()) != null) {
          responseJson.append(line);
        }
        br.close();

        ObjectMapper objectMapper = new ObjectMapper();
        NaverApiResponseDto naverApiResponseDto = objectMapper.readValue(responseJson.toString(),
            NaverApiResponseDto.class);

        for (NaverParsingDto dto : naverApiResponseDto.getItems()) {
          String publisherName = dto.getPublisher();
          Publisher publisher;
          if (publisherRepository.existsByName(publisherName)) {
            publisher = publisherRepository.findByName(publisherName);
          } else {
            publisher = new Publisher();
            publisher.setName(publisherName);
            publisherRepository.save(publisher);
          }

          Book book = Book.builder()
              .title(dto.getTitle())
              .priceStandard(parseLongFromString(dto.getPrice()))
              .priceSales(parseLongFromString(dto.getDiscount()))
              .publisher(publisher)
              .imageUrl(dto.getImage())
              .description(dto.getDescription())
              .longIsbn(parseLongFromString(dto.getIsbn()))
              .publishedDate(LocalDate.parse((StringUtils.hasText(dto.getPubdate()) ? dto.getPubdate() : "22000101"), Formatter.NAVER_DATE_FORMATTER))
              .build();

          bookRepository.save(book);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  private Long parseLongFromString(String value) {
    if (StringUtils.hasText(value)) {
      return Long.parseLong(value);
    } else {
      return null;
    }
  }

}
