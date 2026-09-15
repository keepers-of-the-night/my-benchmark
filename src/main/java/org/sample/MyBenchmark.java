package org.sample;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(3)
public class MyBenchmark
{
    private List<String> words;
    @Param({"2", "4", "6"})
    public int numThreads;

    @Setup
    public void setup() {
        List<String> littleWords = new ArrayList<>(List.of("Hello", "Hi", "hello", "HI", "hi-hi"));
        words = new ArrayList<>(Collections.nCopies(1_000_000, littleWords).stream().flatMap(Collection::stream).toList());
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(MyBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    static Map<String, Integer> countWords(List<String> words) {
        Map<String, Integer> counts = new HashMap<>();

        for(String word : words) {
            counts.merge(word.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }

        return counts;
    }

    @Benchmark
    public Map<String, Integer> withoutThreads() {
        return countWords(words);
    }

    @Benchmark
    public Map<String, Integer> withThreads() throws InterruptedException, ExecutionException {
        int sizeSublist = (words.size() + numThreads - 1) / numThreads;
        List<List<String>> listOfSublists = new ArrayList<>();

        for(int i = 0; i < words.size(); i += sizeSublist) {
            listOfSublists.add(words.subList(i, Math.min(words.size(), sizeSublist + i)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, Integer>>> listOfFutures = new ArrayList<>();

        for(List<String> sublist : listOfSublists) {
            listOfFutures.add(pool.submit(() -> countWords(sublist)));
        }

        Map<String, Integer> total = new ConcurrentHashMap<>();

        for(Future<Map<String, Integer>> future : listOfFutures) {
            Map<String, Integer> partial = future.get();
            partial.forEach((k, v) -> total.merge(k, v, Integer::sum));
        }

        pool.shutdown();

        return total;
    }

    @Benchmark
    public Map<String, Integer> withStream() {
        return words.parallelStream().map((w) -> w.toLowerCase(Locale.ROOT)).collect(Collectors.toConcurrentMap((w) -> w, (w) -> 1, Integer::sum));
    }

    @Benchmark
    public Map<String, Integer> withVirtualThreads() throws InterruptedException, ExecutionException {
        int sizeOfSublists = (words.size() + numThreads - 1) / numThreads;
        List<List<String>> listsOfSublists = new ArrayList<>();

        for(int i = 0; i < words.size(); i += sizeOfSublists)
        {
            listsOfSublists.add(words.subList(i, Math.min(words.size(), sizeOfSublists + i)));
        }

        List<Future<Map<String, Integer>>> futures = new ArrayList<>();
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

        for(List<String> subList : listsOfSublists)
        {
            futures.add(pool.submit(() -> countWords(subList)));
        }

        Map<String, Integer> total = new ConcurrentHashMap<>();

        for(Future<Map<String, Integer>> future : futures)
        {
            future.get().forEach((k, v) -> total.merge(k, v, Integer::sum));
        }

        pool.shutdown();

        return total;
    }

}

