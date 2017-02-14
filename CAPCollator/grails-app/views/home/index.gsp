<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  Public home

  <g:each in="${statsCache}" var="i">
    <h1>${i.key}</h1>
    <g:each in="${i.value}" var="i2">
      <h2>${i2.key}</h2>
      <table>
        <thead>
          <tr>
            <g:each in="${i2.counter_data}" var="v" status="idx">
              <td>
                <g:if test="${idx==i2.current_slot}"><strong>${v}</strong></g:if>
                <g:else>${v}</g:else>
              </td>
            </g:each>
          </tr>
        </thead>
        <tbody>
          <tr>
            <g:each in="${i2.counter_data}" var="v" status="idx">
              <td>
                <g:if test="${idx==i2.current_slot}"><strong>${i2.counter_data[idx]}</strong></g:if>
                <g:else>${i2.counter_data[idx]}</g:else>
              </td>
            </g:each>
          </tr>
        </tbody>
      </table>
    </g:each>
  </g:each>
</body>
</html>
