# URI Resolution

| Base URL                   | Resolution | URI / URI Template        | Result                        |
|----------------------------|------------|---------------------------|-------------------------------|
|`https://example.com`|`RFC`|`null`|`https://example.com`|
|`https://example.com/`|`RFC`|`null`|`https://example.com/`|
|`https://example.com`|`RFC`|(empty)|`https://example.com`|
|`https://example.com/`|`RFC`|(empty)|`https://example.com/`|
|`https://example.com`|`RFC`|`/`|`https://example.com/`|
|`https://example.com/`|`RFC`|`/`|`https://example.com/`|
|`https://example.com`|`RFC`|`https://example.org/foo`|`https://example.org/foo`|
|`https://example.com`|`RFC`|`/foo/bar`|`https://example.com/foo/bar`|
|`https://example.com`|`RFC`|`foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api`|`RFC`|`/foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api`|`RFC`|`foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api/`|`RFC`|`/foo/bar`|`https://example.com/foo/bar`|
|`https://example.com/api/`|`RFC`|`foo/bar`|`https://example.com/api/foo/bar`|
|`null`|`RFC`|`https://example.com/foo`|`https://example.com/foo`|
|`/foo`|`RFC`|`/`|Exception|
|`null`|`RFC`|`null`|Exception|
|`null`|`RFC`|`/foo`|Exception|
|`null`|`RFC`|`foo`|Exception|
|`https://example.com/api`|`APPEND`|`/foo/bar`|`https://example.com/api/foo/bar`|
|`https://example.com/api`|`APPEND`|`foo/bar`|`https://example.com/api/foo/bar`|
|`https://example.com/api/`|`APPEND`|`/foo/bar`|`https://example.com/api/foo/bar`|
|`https://example.com/api/`|`APPEND`|`foo/bar`|`https://example.com/api/foo/bar`|
