package com.candidate.transformer.engine.parser;

import com.candidate.transformer.entity.RawCandidate;
import java.io.InputStream;
import java.util.List;

public interface CandidateParser {
    List<RawCandidate> parse(InputStream inputStream, String fileName);
}
