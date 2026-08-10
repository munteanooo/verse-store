package com.verse.store.api;

import com.verse.store.domain.Verse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/verses")
public class VerseController {

    @GetMapping
    public List<Verse> getAll() {
        return List.of(
                new Verse(
                        1L,
                        "John",
                        3,
                        16,
                        "For God so loved the world..."
                )
        );
    }
}