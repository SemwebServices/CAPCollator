<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  Public home

  <pre>
  ${statsCache} 
  </pre>

  <g:each in="${statsCache}" var="i">
    <h1>${i.key}</h1>
    <g:each in="${i.value}" var="i2">
      <h2>${i2.key} (${i2.value.current_slot} ${i2.value.current_slot?.class?.name})</h2>
      <table>
        <thead>
          <tr>
            <g:each in="${i2.value.counter_data}" var="v" status="idx">
              <td>
                <g:if test="${idx==i2.value.current_slot}"><strong>${idx}</strong></g:if>
                <g:else>${idx}</g:else>
              </td>
            </g:each>
          </tr>
        </thead>
        <tbody>
          <tr>
            <g:each in="${i2.value.counter_data}" var="v" status="idx">
              <td>
                <g:if test="${idx==i2.value.current_slot}"><strong>${v}</strong></g:if>
                <g:else>${v}</g:else>
              </td>
            </g:each>
          </tr>
        </tbody>
      </table>
    </g:each>
  </g:each>
</body>
</html>
