# JMH Benchmark: Word Count

Сравнение производительности разных подходов к подсчёту слов:
- последовательный
- ExecutorService
- parallelStream
- VirtualThreads

## Запуск
mvn clean package
java -jar target/benchmarks.jar