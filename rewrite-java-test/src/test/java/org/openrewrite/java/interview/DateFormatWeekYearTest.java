package org.openrewrite.java.interview;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class DateFormatWeekYearTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DateFormatWeekYear());
    }

    @Test
    void correct_format_is_not_modified() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                      String r = new SimpleDateFormat("yyyy/MM/dd").format(d.toInstant());   // Correct; returns '2015/12/31' as expected
                      r = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(d.toInstant()); // Correct; returns '2015/12/31' as expected
                  }
              }
              """
          )
        );
    }

    @Test
    void incorrect_use_of_week_year_is_fixed() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                      String r = new SimpleDateFormat("YYYY/MM/dd").format(d.toInstant());
                      r = DateTimeFormatter.ofPattern("YYYY/MM/dd").format(d.toInstant());
                  }
              }
              """,
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                      String r = new SimpleDateFormat("yyyy/MM/dd").format(d.toInstant());
                      r = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(d.toInstant());
                  }
              }
              """
          )
        );
    }


    @Test
    @Disabled
    void data_flow_case() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      String format = "yyyy/MM/dd";
                      Date d = new SimpleDateFormat(format).parse("2015/12/31");
                  }
              }
              """,
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                      String r = new SimpleDateFormat("yyyy/MM/dd").format(d.toInstant());
                  }
              }
              """
          )
        );
    }

    @Test
    @Disabled
    void data_flow_case_insane() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      String format = "YYYY/MM/dd";
                      Date d2 = new SimpleDateFormat((DateTimeFormatter.ofPattern(format).format(d.toInstant()))).parse("2015/12/31");
                  }
              }
              """,
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      String format = "yyyy/MM/dd";
                      Date d2 = new SimpleDateFormat((DateTimeFormatter.ofPattern(format).format(d.toInstant()))).parse("2015/12/31");
                  }
              }
              """
          )
        );
    }


    @Test
    @Disabled
    void correct_use_of_week_year_is_unmodified() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              import java.text.SimpleDateFormat;
              import java.time.format.DateTimeFormatter;
              class A {
                  void a() {
                      Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                      String r = new SimpleDateFormat("YYYY-ww").format(d.toInstant()); // Correct, 'Week year' is used with 'Week of year'. r = '2016-01'
                      r = DateTimeFormatter.ofPattern("YYYY-ww").format(d.toInstant()); // Correct; returns '2016-01' as expected
                  }
              }
              """
          )
        );
    }
}
