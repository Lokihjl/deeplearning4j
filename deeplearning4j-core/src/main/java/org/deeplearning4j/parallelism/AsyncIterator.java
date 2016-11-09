package org.deeplearning4j.parallelism;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronous Iterator for better performance of iterators in dl4j-nn & dl4j-nlp
 *
 * @author raver119@gmail.com
 */
@Slf4j
public class AsyncIterator<T extends Object> implements Iterator<T> {
    protected LinkedBlockingQueue<T> buffer;
    protected ReaderThread<T> thread;
    protected Iterator<T> iterator;
    protected T terminator = (T) new Object();
    protected T nextElement;

    public AsyncIterator(@NonNull Iterator<T> iterator, int bufferSize) {
        this.buffer = new LinkedBlockingQueue<>(bufferSize);
        this.iterator = iterator;

        thread = new ReaderThread<>(iterator, this.buffer, terminator);
        thread.start();
    }

    public AsyncIterator(@NonNull Iterator<T> iterator) {
        this(iterator, 1024);
    }

    @Override
    public boolean hasNext() {
        try {
            if (nextElement != null) {
                return true;
            }
            nextElement = buffer.take();
            if (nextElement == terminator)
                return false;
            return true;
        } catch (Exception e) {
            log.error("Premature end of loop!");
            return false;
        }
    }

    @Override
    public T next() {
        T temp = nextElement;
        nextElement = null;
        return temp;
    }

    @Override
    public void remove() {
        // no-op
    }


    private class ReaderThread<T> extends Thread implements Runnable {
        private LinkedBlockingQueue<T> buffer;
        private Iterator<T> iterator;
        private T terminator;

        public ReaderThread(Iterator<T> iterator, LinkedBlockingQueue<T> buffer, T terminator) {
            this.buffer = buffer;
            this.iterator = iterator;
            this.terminator = terminator;

            setDaemon(true);
            setName("AsyncIterator Reader thread");
        }

        @Override
        public void run() {
            try {
                while (iterator.hasNext()) {
                    buffer.put(iterator.next());
                }
                buffer.put(terminator);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
