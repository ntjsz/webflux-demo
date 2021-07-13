package haha;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@RestController
public class TestController {

    @GetMapping("test")
    public Mono<String> test() {

        List<String> l = Collections.synchronizedList(new ArrayList<>());

        Mono<Result> result12 = test1(l, "1")
                .flatMap(andRule(l, "1", "2"));

        Mono<Result> test5 = test1(l, "3")
                .flatMap(orRule(l, "3", "5"));
        ;
        Mono<Result> test7 = test1(l, "7");

        Mono<Result> resultMono = result12.subscribeOn(Schedulers.parallel());
        Mono<Result> resultMono2 = test5.subscribeOn(Schedulers.parallel());
        Mono<Result> resultMono3 = test7.subscribeOn(Schedulers.parallel());
        return Mono.when(resultMono, resultMono2, resultMono3)
                .then(Mono.defer(() -> Mono.just(l.toString())));
    }

    private Mono<Result> test1(List<String> l, String s) {
        return WebClient.create("http://127.0.0.1:8080/test")
                .get()
                .uri(uriBuilder -> uriBuilder.queryParam("name", s).build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    if (s.equals("0") || s.equals("4")) {
                        Result result = new Result(s, false);
                        return Mono.just(result);
                    } else {
                        l.add(response);
                        Result result = new Result(s, true);
                        return Mono.just(result);
                    }
                });
    }

    private static class Result {
        private String s;
        private Boolean success;

        public Result(String s, Boolean success) {
            this.s = s;
            this.success = success;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "s='" + s + '\'' +
                    ", success=" + success +
                    '}';
        }
    }

    Function<? super Result, ? extends Mono<? extends Result>> andRule(List<String> l, String s1, String s2) {
        return (result) -> {
            if (s1.equals(result.s) && result.success) {
                return test1(l, s2);
            } else {
                return Mono.empty();
            }
        };
    }

    Function<? super Result, ? extends Mono<? extends Result>> orRule(List<String> l, String s1, String s2) {
        return (result) -> {
            if (s1.equals(result.s) && (!result.success)) {
                return test1(l, s2);
            } else {
                return Mono.empty();
            }
        };
    }
}
